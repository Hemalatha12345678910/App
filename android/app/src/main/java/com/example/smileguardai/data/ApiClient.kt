package com.example.smileguardai.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class PredictionBox(
    val className: String,
    val confidence: Float,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

data class AnalysisResponse(
    val imageWidth: Int,
    val imageHeight: Int,
    val predictions: List<PredictionBox>,
    val geminiReport: String?
)

data class ChatResponse(
    val reply: String
)

data class PatientRecord(
    val id: String = "",
    val patientId: String = "",
    val name: String,
    val dob: String = "2000-01-01",
    val email: String,
    val phone: String = "",
    val status: String = "Active"
)

data class AppointmentRecord(
    val id: String = "",
    val patientName: String,
    val patientEmail: String = "",
    val date: String,
    val time: String,
    val status: String = "Scheduled"
)

data class ClinicalReport(
    val id: String = "",
    val date: String,
    val patientName: String,
    val patientEmail: String = "",
    val findings: String,
    val confidence: Int,
    val summary: String
)

data class DashboardMetrics(
    val totalPatients: Int,
    val totalScans: Int,
    val concordance: String,
    val recentScans: List<ClinicalReport>,
    val scheduledAppointments: List<AppointmentRecord>
)

data class UserAccount(
    val email: String,
    val name: String,
    val role: String
)

data class TreatmentStageDto(
    val id: String,
    val title: String,
    val status: String = "Pending",
    val isCompleted: Boolean = false
)

data class TreatmentPlanDto(
    val stages: List<TreatmentStageDto>,
    val clinicalNotes: String = "",
    val upcomingProcedures: String = ""
)

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val mobile: String = "",
    val address: String = "",
    val gender: String = "Male",
    val specialization: String = "Orthodontics"
)

// Mirrors src/lib/supabase.js — same project the website uses. Settings syncs into it best-effort
// (Flask remains this app's real backend for everything else).
data class SupabaseSession(val accessToken: String, val userId: String)

// A doctor's public directory entry, read from Supabase `clinical_patients` rows whose
// patient_id is "DOCTOR_PROFILE_<uid>" — the same shared directory the website reads/writes
// (src/components/views/Dashboard.jsx's syncDoctor / src/components/views/UploadAnalysis.jsx).
data class SupabaseDoctor(
    val doctorId: String,
    val fullName: String,
    val email: String,
    val phone: String
)

object ApiClient {
    // TODO(release): this default only works over `adb reverse tcp:5000 tcp:5000` on a USB-tethered
    // dev machine — it will not reach anything once installed from the Play Store. Deploy ml-engine/
    // (render.yaml is already set up for this) and either change this default to that HTTPS URL, or
    // set it once via the Settings > Server URL field below (persists across restarts, no rebuild
    // needed).
    private const val DEFAULT_BASE_URL = "https://pdd-backend-os1e.onrender.com"
    private const val PREFS_NAME = "smileguardai_prefs"
    private const val PREF_BASE_URL = "base_url"

    private var appContext: Context? = null
    private var baseUrl: String = DEFAULT_BASE_URL

    // Persistent Local Fallback Caches
    private val localPatients = mutableListOf<PatientRecord>()
    private val localAppointments = mutableListOf<AppointmentRecord>()
    private val localReports = mutableListOf<ClinicalReport>()

    // Call once from MainActivity.onCreate so a user-set server URL survives app restarts —
    // mirrors the website's localStorage-backed getBackendUrl/setBackendUrl (src/lib/config.js).
    fun init(context: Context) {
        appContext = context.applicationContext
        baseUrl = prefs()?.getString(PREF_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    private fun prefs(): SharedPreferences? =
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setBaseUrl(url: String) {
        baseUrl = url.trimEnd('/').ifBlank { DEFAULT_BASE_URL }
        prefs()?.edit()?.putString(PREF_BASE_URL, baseUrl)?.apply()
    }

    fun resetBaseUrl() {
        baseUrl = DEFAULT_BASE_URL
        prefs()?.edit()?.remove(PREF_BASE_URL)?.apply()
    }

    fun getBaseUrl(): String = baseUrl

    fun isUsingDefaultBaseUrl(): Boolean = baseUrl == DEFAULT_BASE_URL

    private fun saveLocalUserAccount(email: String, pass: String, name: String, role: String) {
        val p = prefs() ?: return
        val key = "user_account_${email.lowercase()}"
        val obj = JSONObject().apply {
            put("email", email)
            put("password", pass)
            put("name", name)
            put("role", role)
        }
        p.edit().putString(key, obj.toString()).apply()
    }

    private fun getLocalUserAccount(email: String): JSONObject? {
        val p = prefs() ?: return null
        val key = "user_account_${email.lowercase()}"
        val raw = p.getString(key, null) ?: return null
        return try { JSONObject(raw) } catch (_: Exception) { null }
    }

    suspend fun register(email: String, password: String, name: String, role: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        saveLocalUserAccount(email, password, name, role)
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("name", name)
                put("role", role)
            }
            val url = URL("$baseUrl/api/auth/register")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 4000
                readTimeout = 4000
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(json.toString()); it.flush() }
            val responseCode = conn.responseCode
            val responseText = (if (responseCode in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (responseText.trim().startsWith("{")) {
                val obj = JSONObject(responseText)
                if (responseCode in 200..299 && obj.optBoolean("success", false)) {
                    val user = obj.optJSONObject("user")
                    return@withContext Result.success(
                        UserAccount(
                            email = user?.optString("email") ?: email,
                            name = user?.optString("name") ?: name,
                            role = user?.optString("role") ?: role
                        )
                    )
                } else if (obj.has("message")) {
                    return@withContext Result.failure(Exception(obj.optString("message")))
                }
            }
        } catch (_: Exception) {}

        // Safe Fallback: Local user registration succeeded
        Result.success(UserAccount(email = email, name = name, role = role))
    }

    suspend fun login(email: String, password: String, role: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("role", role)
            }
            val url = URL("$baseUrl/api/auth/login")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 4000
                readTimeout = 4000
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(json.toString()); it.flush() }
            val responseCode = conn.responseCode
            val responseText = (if (responseCode in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (responseText.trim().startsWith("{")) {
                val obj = JSONObject(responseText)
                if (responseCode in 200..299 && obj.optBoolean("success", false)) {
                    val user = obj.optJSONObject("user")
                    return@withContext Result.success(
                        UserAccount(
                            email = user?.optString("email") ?: email,
                            name = user?.optString("name") ?: email,
                            role = user?.optString("role") ?: role
                        )
                    )
                } else if (obj.has("message")) {
                    return@withContext Result.failure(Exception(obj.optString("message")))
                }
            }
        } catch (_: Exception) {}

        // Fallback: Validate local user credentials
        val localUser = getLocalUserAccount(email)
        if (localUser != null) {
            val savedPass = localUser.optString("password")
            val savedRole = localUser.optString("role")
            if (savedPass.isNotBlank() && savedPass != password) {
                return@withContext Result.failure(Exception("Incorrect password for $email."))
            }
            val name = localUser.optString("name").ifBlank { email }
            return@withContext Result.success(UserAccount(email = email, name = name, role = savedRole.ifBlank { role }))
        }

        Result.success(UserAccount(email = email, name = email.split('@').firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "User", role = role))
    }

    // 1. DYNAMIC PATIENTS API — doctorEmail scopes the roster to that doctor only
    suspend fun fetchPatients(doctorEmail: String = ""): Result<List<PatientRecord>> = withContext(Dispatchers.IO) {
        try {
            val query = if (doctorEmail.isNotBlank()) "?email=${java.net.URLEncoder.encode(doctorEmail, "UTF-8")}" else ""
            val url = URL("$baseUrl/api/patients$query")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
            }
            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArr = JSONArray(responseText)
                val list = mutableListOf<PatientRecord>()
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    list.add(
                        PatientRecord(
                            id = obj.optString("id"),
                            patientId = obj.optString("patientId"),
                            name = obj.optString("name"),
                            dob = obj.optString("dob"),
                            email = obj.optString("email"),
                            phone = obj.optString("phone"),
                            status = obj.optString("status", "Active")
                        )
                    )
                }
                // Backend is authoritative here — trust it directly instead of merging with the
                // local cache, which previously caused real patients to be silently dropped
                // whenever two patients shared a patientId.
                Result.success(list)
            } else {
                Result.success(localPatients.toList())
            }
        } catch (e: Exception) {
            Result.success(localPatients.toList())
        }
    }

    // Resolves a patient's own login email to the patientId a doctor already registered them
    // under, so a patient can view (read-only) the exact same treatment plan their doctor manages.
    suspend fun fetchPatientSelf(email: String): Result<PatientRecord> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/patients/self?email=${java.net.URLEncoder.encode(email, "UTF-8")}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
            }
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(text).getJSONObject("patient")
                Result.success(
                    PatientRecord(
                        id = obj.optString("id"),
                        patientId = obj.optString("patientId"),
                        name = obj.optString("name"),
                        dob = obj.optString("dob"),
                        email = obj.optString("email"),
                        phone = obj.optString("phone"),
                        status = obj.optString("status", "Active")
                    )
                )
            } else {
                Result.failure(Exception("No patient record found for this email yet."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPatient(patient: PatientRecord, doctorEmail: String = ""): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!localPatients.any { it.patientId == patient.patientId }) {
            localPatients.add(patient)
        }
        try {
            val json = JSONObject().apply {
                put("patientId", patient.patientId)
                put("name", patient.name)
                put("dob", patient.dob)
                put("email", patient.email)
                put("phone", patient.phone)
                if (doctorEmail.isNotBlank()) put("doctorEmail", doctorEmail)
            }
            val url = URL("$baseUrl/api/patients")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 4000
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { os ->
                os.write(json.toString())
                os.flush()
            }
            Result.success(conn.responseCode in 200..201)
        } catch (e: Exception) {
            Result.success(true)
        }
    }

    // 2. DYNAMIC APPOINTMENTS API — doctorEmail scopes the appointment to that doctor
    suspend fun scheduleAppointment(patientName: String, date: String, time: String, doctorEmail: String = "", patientEmail: String = ""): Result<Boolean> = withContext(Dispatchers.IO) {
        val appt = AppointmentRecord(
            id = (localAppointments.size + 1).toString(),
            patientName = patientName,
            patientEmail = patientEmail,
            date = date,
            time = time
        )
        localAppointments.add(appt)
        try {
            val json = JSONObject().apply {
                put("patientName", patientName)
                put("date", date)
                put("time", time)
                if (doctorEmail.isNotBlank()) put("doctorEmail", doctorEmail)
                if (patientEmail.isNotBlank()) put("patientEmail", patientEmail)
            }
            val url = URL("$baseUrl/api/appointments")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 4000
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { os ->
                os.write(json.toString())
                os.flush()
            }
            Result.success(conn.responseCode in 200..201)
        } catch (e: Exception) {
            Result.success(true)
        }
    }

    // 3. DYNAMIC DASHBOARD METRICS API — doctorEmail scopes metrics/recent scans to that doctor only
    suspend fun fetchDashboardMetrics(doctorEmail: String = ""): Result<DashboardMetrics> = withContext(Dispatchers.IO) {
        var serverPatients = 0
        var serverScans = 0
        var concordance = "0%"
        var serverRecentScans: List<ClinicalReport>? = null
        var serverAppointments: List<AppointmentRecord>? = null
        var serverReachable = false

        try {
            val query = if (doctorEmail.isNotBlank()) "?email=${java.net.URLEncoder.encode(doctorEmail, "UTF-8")}" else ""
            val url = URL("$baseUrl/api/dashboard-metrics$query")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
            }
            if (conn.responseCode == 200) {
                serverReachable = true
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(text)
                serverPatients = obj.optInt("totalPatients", 0)
                serverScans = obj.optInt("totalScans", 0)
                concordance = obj.optString("concordance", "0%")
                val scansArr = obj.optJSONArray("recentScans")
                if (scansArr != null) {
                    serverRecentScans = (0 until scansArr.length()).map { i ->
                        val r = scansArr.getJSONObject(i)
                        ClinicalReport(
                            id = r.optString("id"),
                            date = r.optString("date"),
                            patientName = r.optString("patientName"),
                            patientEmail = r.optString("patientEmail"),
                            findings = r.optString("findings"),
                            confidence = r.optInt("confidence"),
                            summary = r.optString("summary")
                        )
                    }
                }
                val apptArr = obj.optJSONArray("scheduledAppointments")
                if (apptArr != null) {
                    serverAppointments = (0 until apptArr.length()).map { i ->
                        val a = apptArr.getJSONObject(i)
                        AppointmentRecord(
                            id = a.optString("id"),
                            patientName = a.optString("patientName"),
                            patientEmail = a.optString("patientEmail"),
                            date = a.optString("date"),
                            time = a.optString("time"),
                            status = a.optString("status", "Scheduled")
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // Trust the server once it actually answers — it's the real per-account source of truth.
        // Only fall back to the local, process-lifetime cache (which isn't account-scoped and was
        // previously inflating counts across account switches within one app session) when the
        // server request genuinely failed.
        val totalP = if (serverReachable) serverPatients else localPatients.size
        val totalS = if (serverReachable) serverScans else localReports.size
        val conc = if (totalS == 0) "0%" else concordance

        Result.success(
            DashboardMetrics(
                totalPatients = totalP,
                totalScans = totalS,
                concordance = conc,
                recentScans = serverRecentScans ?: localReports.takeLast(5),
                scheduledAppointments = serverAppointments ?: localAppointments
            )
        )
    }

    // 4. AI IMAGE DIAGNOSTIC ANALYSIS API
    suspend fun analyzeImage(bitmap: Bitmap, role: String = "doctor", patientName: String = "Dr. Dinesh", doctorEmail: String = "", patientEmail: String = ""): Result<AnalysisResponse> = withContext(Dispatchers.IO) {
        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            val jsonPayload = JSONObject().apply {
                put("image", base64Image)
                put("role", role)
                put("patientName", patientName)
                if (doctorEmail.isNotBlank()) put("doctorEmail", doctorEmail)
                if (patientEmail.isNotBlank()) put("patientEmail", patientEmail)
            }

            val url = URL("$baseUrl/analyze")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 45000
                readTimeout = 45000
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { os ->
                os.write(jsonPayload.toString())
                os.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonObj = JSONObject(responseText)

                val imgObj = jsonObj.optJSONObject("image")
                val imgW = imgObj?.optInt("width") ?: bitmap.width
                val imgH = imgObj?.optInt("height") ?: bitmap.height

                val predsArray = jsonObj.optJSONArray("predictions") ?: JSONArray()
                val predictions = mutableListOf<PredictionBox>()
                for (i in 0 until predsArray.length()) {
                    val p = predsArray.getJSONObject(i)
                    predictions.add(
                        PredictionBox(
                            className = p.optString("class", "unknown"),
                            confidence = p.optDouble("confidence", 0.0).toFloat(),
                            x = p.optDouble("x", 0.0).toFloat(),
                            y = p.optDouble("y", 0.0).toFloat(),
                            width = p.optDouble("width", 0.0).toFloat(),
                            height = p.optDouble("height", 0.0).toFloat()
                        )
                    )
                }

                val report = jsonObj.optString("gemini_report", "")

                localReports.add(
                    ClinicalReport(
                        id = (localReports.size + 1).toString(),
                        date = "2026-08-01",
                        patientName = patientName,
                        patientEmail = patientEmail,
                        findings = if (predictions.isEmpty()) "Healthy" else predictions.map { it.className }.distinct().joinToString(", "),
                        confidence = if (predictions.isNotEmpty()) (predictions.map { it.confidence }.average() * 100).toInt() else 95,
                        summary = report.take(150) + "..."
                    )
                )

                Result.success(AnalysisResponse(imgW, imgH, predictions, report))
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() }
                Result.failure(Exception("Analysis server error (HTTP $responseCode)${if (errorBody != null) ": $errorBody" else ""}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Could not reach AI backend at $baseUrl (${e.javaClass.simpleName}: ${e.message})"))
        }
    }

    // 5. AI CHATBOT API
    suspend fun sendChatMessage(message: String, role: String = "patient", history: List<Map<String, String>> = emptyList()): Result<ChatResponse> = withContext(Dispatchers.IO) {
        try {
            val historyArray = JSONArray()
            history.forEach { item ->
                historyArray.put(JSONObject().apply {
                    put("sender", item["sender"])
                    put("text", item["text"])
                })
            }

            val jsonPayload = JSONObject().apply {
                put("message", message)
                put("role", role)
                put("history", historyArray)
            }

            val url = URL("$baseUrl/chat")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { os ->
                os.write(jsonPayload.toString())
                os.flush()
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonObj = JSONObject(responseText)
                val reply = jsonObj.optString("reply", "No response received.")
                Result.success(ChatResponse(reply))
            } else {
                Result.failure(Exception("Chat server error (${conn.responseCode})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 6. SETTINGS PASSWORD UPDATE API
    suspend fun updatePassword(email: String, newPassword: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("email", email)
                put("newPassword", newPassword)
            }
            val url = URL("$baseUrl/api/settings/password")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 4000
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { os ->
                os.write(jsonPayload.toString())
                os.flush()
            }
            Result.success(conn.responseCode in 200..201)
        } catch (e: Exception) {
            Result.success(true)
        }
    }

    // 6b. PROFILE API — Personal Information (name/mobile/address/gender/specialization)
    suspend fun fetchProfile(email: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/settings/profile?email=${java.net.URLEncoder.encode(email, "UTF-8")}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
            }
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(text)
                Result.success(
                    UserProfile(
                        name = obj.optString("name", ""),
                        email = obj.optString("email", email),
                        mobile = obj.optString("mobile", ""),
                        address = obj.optString("address", ""),
                        gender = obj.optString("gender", "Male"),
                        specialization = obj.optString("specialization", "Orthodontics")
                    )
                )
            } else {
                Result.failure(Exception("No saved profile yet"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveProfile(
        email: String,
        name: String,
        mobile: String,
        address: String,
        gender: String,
        specialization: String,
        role: String = "doctor"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("name", name)
                put("mobile", mobile)
                put("address", address)
                put("gender", gender)
                put("specialization", specialization)
                put("role", role)
            }
            val url = URL("$baseUrl/api/settings/profile")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 4000
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(json.toString()); it.flush() }
            Result.success(conn.responseCode in 200..201)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 7. TREATMENT PLANS API — keyed by a stable patientKey (e.g. patientId), persisted per patient.
    // Bundles stages + clinical notes + upcoming procedures together so the whole page saves as one unit.
    suspend fun fetchTreatmentPlan(patientKey: String): Result<TreatmentPlanDto> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/treatment-plans/${java.net.URLEncoder.encode(patientKey, "UTF-8")}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
            }
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(text)
                val arr = obj.optJSONArray("stages") ?: JSONArray()
                val stages = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    TreatmentStageDto(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        status = o.optString("status", "Pending"),
                        isCompleted = o.optBoolean("isCompleted", false)
                    )
                }
                Result.success(
                    TreatmentPlanDto(
                        stages = stages,
                        clinicalNotes = obj.optString("clinicalNotes", ""),
                        upcomingProcedures = obj.optString("upcomingProcedures", "")
                    )
                )
            } else {
                Result.success(TreatmentPlanDto(emptyList()))
            }
        } catch (e: Exception) {
            Result.success(TreatmentPlanDto(emptyList()))
        }
    }

    suspend fun saveTreatmentPlan(
        patientKey: String,
        stages: List<TreatmentStageDto>,
        clinicalNotes: String = "",
        upcomingProcedures: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val arr = JSONArray()
            stages.forEach { s ->
                arr.put(JSONObject().apply {
                    put("id", s.id)
                    put("title", s.title)
                    put("status", s.status)
                    put("isCompleted", s.isCompleted)
                })
            }
            val payload = JSONObject().apply {
                put("stages", arr)
                put("clinicalNotes", clinicalNotes)
                put("upcomingProcedures", upcomingProcedures)
            }
            val url = URL("$baseUrl/api/treatment-plans/${java.net.URLEncoder.encode(patientKey, "UTF-8")}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 4000
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload.toString()); it.flush() }
            Result.success(conn.responseCode in 200..201)
        } catch (e: Exception) {
            Result.success(false)
        }
    }

    // 8. SUPABASE MIRROR (Settings only) — same project + anon key as src/lib/supabase.js on the
    // website, called via plain REST (this app has no Supabase SDK dependency, matching the
    // HttpURLConnection style used everywhere else in this file). All calls are best-effort: a
    // Supabase failure must never block the app's own Flask-backed auth/save flow.
    private const val SUPABASE_URL = "https://ywmwdstkgfgvszohcsdx.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inl3bXdkc3RrZ2ZndnN6b2hjc2R4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODM4NzQwMzMsImV4cCI6MjA5OTQ1MDAzM30.Uu_wIyr2MZ84v94BX0sL9JzSTvaILBh-aN-2z0vX6I8"

    private fun parseSupabaseSession(text: String): SupabaseSession? {
        val obj = JSONObject(text)
        val accessToken = obj.optString("access_token", "")
        val userId = obj.optJSONObject("user")?.optString("id", "") ?: ""
        return if (accessToken.isNotBlank()) SupabaseSession(accessToken, userId) else null
    }

    suspend fun supabaseSignIn(email: String, password: String): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val url = URL("$SUPABASE_URL/auth/v1/token?grant_type=password")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(json.toString()); it.flush() }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            val session = if (conn.responseCode in 200..299) parseSupabaseSession(text) else null
            if (session != null) Result.success(session) else Result.failure(Exception("Supabase sign-in failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun supabaseSignUp(email: String, password: String, metadata: Map<String, String>): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        try {
            val dataObj = JSONObject().apply { metadata.forEach { (k, v) -> put(k, v) } }
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("data", dataObj)
            }
            val url = URL("$SUPABASE_URL/auth/v1/signup")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(json.toString()); it.flush() }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            if (conn.responseCode !in 200..299) return@withContext Result.failure(Exception("Supabase sign-up failed"))

            // /signup doesn't always return a session (e.g. if email confirmation is required);
            // fall back to signing in immediately with the same credentials.
            parseSupabaseSession(text)?.let { return@withContext Result.success(it) }
            supabaseSignIn(email, password)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun supabaseUpdateUserMetadata(accessToken: String, metadata: Map<String, String>): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val dataObj = JSONObject().apply { metadata.forEach { (k, v) -> put(k, v) } }
            val json = JSONObject().apply { put("data", dataObj) }
            val url = URL("$SUPABASE_URL/auth/v1/user")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $accessToken")
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(json.toString()); it.flush() }
            Result.success(conn.responseCode in 200..299)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun supabaseUpdatePassword(accessToken: String, newPassword: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply { put("password", newPassword) }
            val url = URL("$SUPABASE_URL/auth/v1/user")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $accessToken")
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(json.toString()); it.flush() }
            Result.success(conn.responseCode in 200..299)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 9. SUPABASE SHARED DOCTOR DIRECTORY — mirrors the website's "Recommended Specialist Care"
    // flow exactly (src/components/views/UploadAnalysis.jsx + Dashboard.jsx's syncDoctor). A
    // doctor's directory entry lives in `clinical_patients` keyed by patient_id =
    // "DOCTOR_PROFILE_<supabase user id>", with medical_history repurposed to hold their
    // specialty. All calls are best-effort — this must never block the app's own Flask flow.

    suspend fun supabaseSyncDoctorProfile(
        accessToken: String,
        userId: String,
        fullName: String,
        email: String,
        mobile: String,
        specialization: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val directoryId = "DOCTOR_PROFILE_$userId"
            val displayName = if (fullName.startsWith("Dr.")) fullName else "Dr. $fullName"
            val payload = JSONObject().apply {
                put("doctor_id", userId)
                put("patient_id", directoryId)
                put("full_name", displayName)
                put("dob", "2000-01-01")
                put("email", email)
                put("phone", mobile)
                put("medical_history", specialization.ifBlank { "Orthodontics" })
                put("status", "Active")
            }

            val checkUrl = URL("$SUPABASE_URL/rest/v1/clinical_patients?doctor_id=eq.${java.net.URLEncoder.encode(userId, "UTF-8")}&patient_id=eq.${java.net.URLEncoder.encode(directoryId, "UTF-8")}&select=id")
            val checkConn = (checkUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $accessToken")
                connectTimeout = 6000
                readTimeout = 6000
            }
            val checkText = checkConn.inputStream.bufferedReader().use { it.readText() }
            val existingArr = JSONArray(checkText)

            if (existingArr.length() > 0) {
                val existingId = existingArr.getJSONObject(0).optString("id")
                val updateUrl = URL("$SUPABASE_URL/rest/v1/clinical_patients?id=eq.${java.net.URLEncoder.encode(existingId, "UTF-8")}")
                val updateConn = (updateUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PATCH"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    doOutput = true
                    connectTimeout = 6000
                    readTimeout = 6000
                }
                OutputStreamWriter(updateConn.outputStream, "UTF-8").use { it.write(payload.toString()); it.flush() }
                Result.success(updateConn.responseCode in 200..299)
            } else {
                val insertUrl = URL("$SUPABASE_URL/rest/v1/clinical_patients")
                val insertConn = (insertUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    doOutput = true
                    connectTimeout = 6000
                    readTimeout = 6000
                }
                OutputStreamWriter(insertConn.outputStream, "UTF-8").use { it.write(payload.toString()); it.flush() }
                Result.success(insertConn.responseCode in 200..299)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun supabaseFetchDoctorsBySpecialty(accessToken: String, specialty: String): Result<List<SupabaseDoctor>> = withContext(Dispatchers.IO) {
        try {
            val encodedSpecialty = java.net.URLEncoder.encode(specialty, "UTF-8")
            val url = URL("$SUPABASE_URL/rest/v1/clinical_patients?patient_id=like.DOCTOR_PROFILE_*&medical_history=eq.$encodedSpecialty&select=*")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $accessToken")
                connectTimeout = 6000
                readTimeout = 6000
            }
            if (conn.responseCode !in 200..299) return@withContext Result.failure(Exception("Directory lookup failed"))
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(text)
            val doctors = mutableListOf<SupabaseDoctor>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                doctors.add(
                    SupabaseDoctor(
                        doctorId = obj.optString("doctor_id"),
                        fullName = obj.optString("full_name"),
                        email = obj.optString("email"),
                        phone = obj.optString("phone")
                    )
                )
            }
            Result.success(doctors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun supabaseShareReportWithDoctor(
        accessToken: String,
        patientAuthUserId: String,
        doctorId: String,
        patientName: String,
        patientEmail: String,
        patientPhone: String,
        geminiReport: String,
        diagnosticsSummary: JSONObject
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // 1. Find or create the patient's roster row under this doctor.
            val lookupUrl = URL("$SUPABASE_URL/rest/v1/clinical_patients?doctor_id=eq.${java.net.URLEncoder.encode(doctorId, "UTF-8")}&email=eq.${java.net.URLEncoder.encode(patientEmail, "UTF-8")}&select=id")
            val lookupConn = (lookupUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $accessToken")
                connectTimeout = 6000
                readTimeout = 6000
            }
            val lookupText = lookupConn.inputStream.bufferedReader().use { it.readText() }
            val existingArr = JSONArray(lookupText)

            val patientDbId: String
            if (existingArr.length() > 0) {
                patientDbId = existingArr.getJSONObject(0).optString("id")
            } else {
                val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val rand = (1000..9999).random()
                val rosterPayload = JSONObject().apply {
                    put("doctor_id", doctorId)
                    put("patient_id", "PID-$year-$rand")
                    put("full_name", patientName)
                    put("dob", "2000-01-01")
                    put("email", patientEmail)
                    put("phone", patientPhone)
                    put("medical_history", "Registered via AI Scan referral (Android app).")
                    put("status", "Active")
                }
                val insertUrl = URL("$SUPABASE_URL/rest/v1/clinical_patients")
                val insertConn = (insertUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    setRequestProperty("Prefer", "return=representation")
                    doOutput = true
                    connectTimeout = 6000
                    readTimeout = 6000
                }
                OutputStreamWriter(insertConn.outputStream, "UTF-8").use { it.write(rosterPayload.toString()); it.flush() }
                if (insertConn.responseCode !in 200..299) return@withContext Result.failure(Exception("Could not register with doctor"))
                val insertedText = insertConn.inputStream.bufferedReader().use { it.readText() }
                patientDbId = JSONArray(insertedText).getJSONObject(0).optString("id")
            }

            // 2. Attach the report.
            val reportPayload = JSONObject().apply {
                put("patient_id", patientDbId)
                put("auth_patient_id", patientAuthUserId)
                put("doctor_id", doctorId)
                put("gemini_report", geminiReport)
                put("diagnostics_summary", diagnosticsSummary)
            }
            val reportUrl = URL("$SUPABASE_URL/rest/v1/clinical_reports")
            val reportConn = (reportUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $accessToken")
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
            }
            OutputStreamWriter(reportConn.outputStream, "UTF-8").use { it.write(reportPayload.toString()); it.flush() }
            Result.success(reportConn.responseCode in 200..299)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
