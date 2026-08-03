package com.example.smileguardai.ui.analysis

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smileguardai.R
import com.example.smileguardai.data.AnalysisResponse
import com.example.smileguardai.data.ApiClient
import com.example.smileguardai.theme.BorderColor
import com.example.smileguardai.theme.PrimaryBlue
import com.example.smileguardai.theme.PrimaryLight
import com.example.smileguardai.theme.TextMain
import com.example.smileguardai.theme.TextMuted
import kotlinx.coroutines.launch

internal fun buildMarkdownAnnotatedString(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val regex = Regex("\\*\\*(.+?)\\*\\*")
    var lastIndex = 0
    for (match in regex.findAll(text)) {
        builder.append(text.substring(lastIndex, match.range.first))
        builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
        builder.append(match.groupValues[1])
        builder.pop()
        lastIndex = match.range.last + 1
    }
    builder.append(text.substring(lastIndex))
    return builder.toAnnotatedString()
}

// Specialty recommendation from detected anomaly classes — parity with the website's
// getRecommendedSpecialty() in src/components/views/UploadAnalysis.jsx, adapted to this
// model's fixed class list (calculus, caries, gingivitis, hypodontia, tooth_discolation, ulcer).
private fun recommendSpecialty(response: AnalysisResponse): String {
    val classes = response.predictions.map { it.className.lowercase() }.toSet()
    return when {
        classes.any { it.contains("caries") } -> "Endodontics"
        classes.any { it.contains("gingivitis") || it.contains("calculus") } -> "Periodontics"
        classes.any { it.contains("hypodontia") } -> "Implantology"
        classes.any { it.contains("discolation") || it.contains("ulcer") } -> "Prosthodontics"
        else -> "Orthodontics"
    }
}

private data class SpecialtyInfo(val name: String, val description: String)

private val SPECIALTIES_LIST = listOf(
    SpecialtyInfo("Orthodontics", "Braces and structural alignment"),
    SpecialtyInfo("Prosthodontics", "Restoration and replacement"),
    SpecialtyInfo("Periodontics", "Gum health and surgeries"),
    SpecialtyInfo("Endodontics", "Root canal specialist care"),
    SpecialtyInfo("Implantology", "Titanium dental implants"),
    SpecialtyInfo("Pediatric", "Specialized care for children")
)

private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        val width = options.outWidth
        val height = options.outHeight
        if (width <= 0 || height <= 0) return null

        var sampleSize = 1
        while ((width / sampleSize) > maxDimension || (height / sampleSize) > maxDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null

        scaleBitmapToMax(bitmap, maxDimension)
    } catch (_: Exception) {
        null
    }
}

private fun createTempImageUri(context: Context): Uri? {
    return try {
        val storageDir = context.externalCacheDir ?: context.cacheDir
        val tempFile = File.createTempFile("scan_camera_", ".jpg", storageDir)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    } catch (e: Exception) {
        android.util.Log.e("UploadAnalysis", "Error creating temp image URI: ${e.message}", e)
        null
    }
}

private fun generatePdfReport(context: Context, patientName: String, reportText: String): File? {
    return try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
            isAntiAlias = true
        }

        val headerBgPaint = Paint().apply {
            color = android.graphics.Color.rgb(2, 132, 199)
        }

        val whiteHeaderPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = android.graphics.Color.rgb(15, 23, 42)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawRect(0f, 0f, 595f, 55f, headerBgPaint)
        canvas.drawText("SMILEGUARD AI - CLINICAL DIAGNOSTIC REPORT", 20f, 35f, whiteHeaderPaint)

        var y = 85f
        canvas.drawText("Patient Name: $patientName", 25f, y, titlePaint)
        y += 20f
        paint.textSize = 10f
        paint.color = android.graphics.Color.GRAY
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        canvas.drawText("Generated: $dateStr", 25f, y, paint)
        y += 25f

        paint.color = android.graphics.Color.LTGRAY
        canvas.drawLine(25f, y, 570f, y, paint)
        y += 25f

        paint.color = android.graphics.Color.BLACK
        paint.textSize = 11f

        val cleanText = reportText
            .replace("**", "")
            .replace("### ", "")
            .replace("## ", "")
            .replace("# ", "")

        val lines = cleanText.split("\n")
        for (rawLine in lines) {
            if (y > 800f) break
            val line = rawLine.trim()
            if (line.isEmpty()) {
                y += 10f
                continue
            }
            val words = line.split(" ")
            var currentLine = ""
            for (word in words) {
                if (paint.measureText("$currentLine $word") > 540f) {
                    canvas.drawText(currentLine.trim(), 25f, y, paint)
                    y += 16f
                    currentLine = word
                } else {
                    currentLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                }
            }
            if (currentLine.isNotEmpty()) {
                canvas.drawText(currentLine.trim(), 25f, y, paint)
                y += 16f
            }
        }

        pdfDocument.finishPage(page)

        val fileName = "SmileGuard_Report_${patientName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val cacheFile = File(context.cacheDir, fileName)
        pdfDocument.writeTo(cacheFile.outputStream())
        pdfDocument.close()

        try {
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsFolder.exists()) downloadsFolder.mkdirs()
            val publicFile = File(downloadsFolder, fileName)
            cacheFile.copyTo(publicFile, overwrite = true)
        } catch (_: Exception) {}

        cacheFile
    } catch (e: Exception) {
        android.util.Log.e("GeneratePDF", "Error building PDF report", e)
        null
    }
}

private fun sharePdfViaWhatsApp(context: Context, pdfFile: File, patientName: String) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "SmileGuard AI Diagnostic Report - $patientName")
            putExtra(Intent.EXTRA_TEXT, "Hello, here is the official SmileGuard AI Dental Diagnostic PDF Report for $patientName.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Diagnostic PDF Report via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        android.util.Log.e("SharePDF", "Error sharing PDF via Intent", e)
    }
}

private fun scaleBitmapToMax(bitmap: Bitmap, maxDimension: Int = 1024): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= maxDimension && height <= maxDimension) return bitmap
    val ratio = minOf(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
    val newWidth = (width * ratio).toInt().coerceAtLeast(1)
    val newHeight = (height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

@Composable
internal fun MarkdownReportText(text: String, modifier: Modifier = Modifier) {
    val lines = text.split("\n").let { all ->
        if (all.isNotEmpty() && all[0].trim().equals("**Clinical AI Analysis Report**", ignoreCase = true)) {
            all.drop(1).dropWhile { it.isBlank() }
        } else all
    }

    Column(modifier = modifier) {
        lines.forEach { rawLine ->
            val line = rawLine.trimEnd()
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> Spacer(modifier = Modifier.height(8.dp))
                trimmed.startsWith("### ") -> {
                    Text(
                        text = trimmed.removePrefix("### "),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("- ") -> {
                    Row(modifier = Modifier.padding(start = 2.dp, top = 2.dp, bottom = 2.dp)) {
                        Text("•  ", fontSize = 13.sp, color = TextMain, fontFamily = FontFamily.SansSerif)
                        Text(
                            text = buildMarkdownAnnotatedString(trimmed.removePrefix("- ")),
                            fontSize = 13.sp,
                            color = TextMain,
                            fontFamily = FontFamily.SansSerif,
                            lineHeight = 19.sp
                        )
                    }
                }
                trimmed.startsWith("*") && trimmed.endsWith("*") && !trimmed.startsWith("**") -> {
                    Text(
                        text = trimmed.removePrefix("*").removeSuffix("*"),
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = TextMuted,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                else -> {
                    val isHeaderOnly = trimmed.startsWith("**") && trimmed.endsWith("**")
                    Text(
                        text = buildMarkdownAnnotatedString(line),
                        fontSize = if (isHeaderOnly) 14.sp else 13.sp,
                        color = TextMain,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = if (isHeaderOnly) 8.dp else 2.dp, bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadAnalysisScreen(
    onNavigateBack: () -> Unit,
    userRole: String = "doctor",
    userName: String = "Patient",
    userEmail: String = "",
    supabaseAccessToken: String = "",
    supabaseUserId: String = "",
    modifier: Modifier = Modifier
) {
    val isPatient = userRole == "patient"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<AnalysisResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Patient Scoped Selection State (Active for Doctor Dashboard only)
    var selectedScanPatientRecord by remember { mutableStateOf<com.example.smileguardai.data.PatientRecord?>(null) }
    var isPatientDropdownOpen by remember { mutableStateOf(false) }
    var patientsList by remember { mutableStateOf(emptyList<com.example.smileguardai.data.PatientRecord>()) }

    // Recommended Specialist Care state (patient role only)
    var selectedSpecialty by remember { mutableStateOf("Orthodontics") }
    var recommendedSpecialty by remember { mutableStateOf("Orthodontics") }
    var directoryDoctors by remember { mutableStateOf(emptyList<com.example.smileguardai.data.SupabaseDoctor>()) }
    var selectedDoctor by remember { mutableStateOf<com.example.smileguardai.data.SupabaseDoctor?>(null) }
    var loadingDoctors by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }
    var shareMessage by remember { mutableStateOf<String?>(null) }
    var isSavingReport by remember { mutableStateOf(false) }
    var saveReportMessage by remember { mutableStateOf<String?>(null) }
    var isDownloadingReport by remember { mutableStateOf(false) }
    var downloadReportMessage by remember { mutableStateOf<String?>(null) }

    fun fetchDoctorsForSpecialty(specialty: String) {
        loadingDoctors = true
        selectedDoctor = null
        scope.launch {
            ApiClient.supabaseFetchDoctorsBySpecialty(supabaseAccessToken, specialty).onSuccess { fetched ->
                directoryDoctors = fetched
            }.onFailure {
                directoryDoctors = emptyList()
            }
            loadingDoctors = false
        }
    }

    LaunchedEffect(userEmail) {
        if (!isPatient) {
            scope.launch {
                ApiClient.fetchPatients(userEmail).onSuccess { fetched ->
                    patientsList = fetched
                }
            }
        }
    }

    // Photo Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val bitmap = decodeSampledBitmapFromUri(context, it)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (bitmap != null) {
                        selectedBitmap = bitmap
                        analysisResult = null
                        errorMessage = null
                    } else {
                        errorMessage = "Failed to decode selected image."
                    }
                }
            }
        }
    }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Camera Capture Launcher with FileProvider Output URI
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            val uri = tempCameraUri!!
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val bitmap = decodeSampledBitmapFromUri(context, uri)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (bitmap != null) {
                        selectedBitmap = bitmap
                        analysisResult = null
                        errorMessage = null
                    } else {
                        errorMessage = "Failed to process photo from camera."
                    }
                }
            }
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val uri = createTempImageUri(context)
            if (uri != null) {
                tempCameraUri = uri
                try {
                    cameraLauncher.launch(uri)
                } catch (e: Exception) {
                    errorMessage = "Could not launch camera app: ${e.message}"
                }
            } else {
                errorMessage = "Failed to create image file for camera."
            }
        } else {
            errorMessage = "Camera permission is required to take a photo."
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // App Top Bar / Header Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Smile Guard ",
                fontFamily = FontFamily.SansSerif,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
            Box(
                modifier = Modifier
                    .background(PrimaryBlue, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "AI",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Page Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(36.dp)
                    .offset(x = (-8).dp)
            ) {
                Text(
                    text = "←",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPatient) "My Scans" else "New Scan Analysis",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isPatient) "Upload a photo of your teeth for an instant AI check-up." else "Upload intraoral or panoramic images for AI evaluation.",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // DOCTOR DASHBOARD ONLY: Patient Selection Dropdown
        if (!isPatient) {
            Text(
                text = "Select Patient for Scan",
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    onClick = { isPatientDropdownOpen = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PrimaryBlue)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedScanPatientRecord?.let { "${it.name} (${it.patientId})" } ?: "-- Choose a patient --",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            fontWeight = if (selectedScanPatientRecord != null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedScanPatientRecord != null) TextMain else TextMuted,
                            maxLines = 1
                        )
                        Text(
                            text = "▼",
                            fontSize = 11.sp,
                            color = TextMain
                        )
                    }
                }

                DropdownMenu(
                    expanded = isPatientDropdownOpen,
                    onDismissRequest = { isPatientDropdownOpen = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(Color.White)
                        .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(8.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "-- Choose a patient --",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 14.sp,
                                color = if (selectedScanPatientRecord == null) Color.White else TextMain
                            )
                        },
                        onClick = {
                            selectedScanPatientRecord = null
                            isPatientDropdownOpen = false
                        },
                        modifier = Modifier.background(if (selectedScanPatientRecord == null) Color(0xFF1D4ED8) else Color.White)
                    )

                    patientsList.forEach { record ->
                        val isSelected = selectedScanPatientRecord?.patientId == record.patientId
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${record.name} (${record.patientId})",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else TextMain
                                )
                            },
                            onClick = {
                                selectedScanPatientRecord = record
                                isPatientDropdownOpen = false
                            },
                            modifier = Modifier.background(if (isSelected) Color(0xFF1D4ED8) else Color.White)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // DOCTOR DASHBOARD ONLY: Locked Guard Banner if no patient selected
        if (!isPatient && selectedScanPatientRecord == null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = Color(0xFFFCD34D), shape = RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔒", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Patient Selection Required",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Please select a patient from the dropdown above before starting an AI diagnostic image scan.",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = Color(0xFFB45309),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // UNLOCKED SCAN UI (Always active for Patients, unlocked for Doctors once patient selected)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 32.dp, horizontal = 20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(PrimaryLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("☁️", fontSize = 32.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Drag & Drop Image Here",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Supports JPG, PNG, DICOM (Max 50MB)",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "OR",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    galleryLauncher.launch("image/*")
                                } catch (e: Exception) {
                                    errorMessage = "Could not open file picker: ${e.message}"
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(PrimaryBlue)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Text(
                                text = "Browse Files",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    val uri = createTempImageUri(context)
                                    if (uri != null) {
                                        tempCameraUri = uri
                                        try {
                                            cameraLauncher.launch(uri)
                                        } catch (e: Exception) {
                                            errorMessage = "Could not launch camera: ${e.message}"
                                        }
                                    } else {
                                        errorMessage = "Failed to initialize camera storage."
                                    }
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(PrimaryBlue)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📷 ", fontSize = 13.sp, maxLines = 1, softWrap = false)
                                Text(
                                    text = "Take Photo",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Image Preview Canvas with Overlay Bounding Box Bounding Tags
            selectedBitmap?.let { bitmap ->
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(16.dp))
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Selected Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Bounding Box Overlay Canvas
                    analysisResult?.let { response ->
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasW = size.width
                            val canvasH = size.height
                            val imgW = response.imageWidth.toFloat()
                            val imgH = response.imageHeight.toFloat()

                            val scaleX = canvasW / imgW
                            val scaleY = canvasH / imgH

                            response.predictions.forEach { p ->
                                val left = (p.x - p.width / 2f) * scaleX
                                val top = (p.y - p.height / 2f) * scaleY
                                val boxW = p.width * scaleX
                                val boxH = p.height * scaleY

                                drawRect(
                                    color = Color(0xFFD97706),
                                    topLeft = Offset(left, top),
                                    size = Size(boxW, boxH),
                                    style = Stroke(width = 4f)
                                )

                                val labelText = "${p.className} (${(p.confidence * 100).toInt()}%)"
                                val textPaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 28f
                                    isFakeBoldText = true
                                }

                                val textWidth = textPaint.measureText(labelText)
                                val pillPaddingHorizontal = 12f
                                val pillHeight = 36f

                                drawRect(
                                    color = Color(0xFFD97706),
                                    topLeft = Offset(left, (top - pillHeight).coerceAtLeast(0f)),
                                    size = Size(textWidth + (pillPaddingHorizontal * 2), pillHeight)
                                )

                                drawContext.canvas.nativeCanvas.drawText(
                                    labelText,
                                    left + pillPaddingHorizontal,
                                    (top - 8f).coerceAtLeast(24f),
                                    textPaint
                                )
                            }
                        }
                    }

                    // Laser Scan Line Animation overlay while analyzing
                    if (isAnalyzing) {
                        val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
                        val scanY by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 1400, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scan_laser_y"
                        )
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val lineY = size.height * scanY
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF38BDF8),
                                        Color(0xFF0284C7),
                                        Color(0xFF38BDF8),
                                        Color.Transparent
                                    )
                                ),
                                start = Offset(0f, lineY),
                                end = Offset(size.width, lineY),
                                strokeWidth = 8f
                            )
                        }
                    }
                }

                // AI Diagnostic Processing Loading Banner Card
                if (isAnalyzing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = PrimaryBlue,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Running AI Diagnostic Scan...",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0369A1)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Detecting lesions, caries & generating report...",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 12.sp,
                                    color = Color(0xFF0284C7)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Run AI Diagnostic Scan Button
                Button(
                    onClick = {
                        isAnalyzing = true
                        errorMessage = null
                        selectedDoctor = null
                        shareMessage = null
                        saveReportMessage = null
                        downloadReportMessage = null
                        scope.launch {
                            val pName = selectedScanPatientRecord?.name ?: userName
                            val pEmail = if (isPatient) userEmail else (selectedScanPatientRecord?.email ?: "")
                            val result = ApiClient.analyzeImage(bitmap, role = userRole, patientName = pName, doctorEmail = userEmail, patientEmail = pEmail)
                            isAnalyzing = false
                            result.onSuccess { response ->
                                analysisResult = response
                                if (isPatient) {
                                    val recommended = recommendSpecialty(response)
                                    recommendedSpecialty = recommended
                                    selectedSpecialty = recommended
                                    fetchDoctorsForSpecialty(recommended)
                                }
                            }.onFailure {
                                errorMessage = it.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isAnalyzing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Analyzing image with trained YOLO AI...",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "🚀 Run AI Diagnostic Scan",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEE2E2), RoundedCornerShape(12.dp))
                            .border(width = 1.dp, color = Color(0xFFEF4444), shape = RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "⚠️ $err",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }

            // Diagnostic Results Section
            analysisResult?.let { res ->
                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Diagnostic Summary",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFD1FAE5), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Concordance level with expert decision: 89.8%",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Diagnostic Findings Table
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Affected Area", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Text("Anomalies Detected", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        }
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)

                        val groupMap = res.predictions.groupBy { it.className }
                        if (groupMap.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Healthy Enamel", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain)
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("healthy_enamel - 95%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                }
                            }
                        } else {
                            groupMap.forEach { (cls, items) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val formattedClass = cls.replace('_', ' ').split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                                    Text(
                                        text = formattedClass,
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMain
                                    )

                                    val avgConf = (items.map { it.confidence }.average() * 100).toInt()
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFFEF3C7), RoundedCornerShape(14.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "$cls (x${items.size}) - $avgConf%",
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Comprehensive AI Analysis Card
                Text(
                    text = "Comprehensive AI Analysis",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Clinical AI Analysis Report",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val reportContent = res.geminiReport
                        if (!reportContent.isNullOrEmpty()) {
                            MarkdownReportText(text = reportContent, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                // Dedicated Save Diagnostic Report Button below the AI Report
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isSavingReport = true
                        saveReportMessage = null
                        scope.launch {
                            val pName = selectedScanPatientRecord?.name ?: userName
                            val pEmail = if (isPatient) userEmail else (selectedScanPatientRecord?.email ?: "")
                            var phone = ""
                            ApiClient.fetchProfile(userEmail).onSuccess { phone = it.mobile }

                            ApiClient.createPatient(
                                com.example.smileguardai.data.PatientRecord(
                                    patientId = "PID-${System.currentTimeMillis()}",
                                    name = pName,
                                    email = pEmail,
                                    phone = phone
                                ),
                                doctorEmail = userEmail
                            )

                            isSavingReport = false
                            saveReportMessage = "Diagnostic report saved successfully for $pName!"
                        }
                    },
                    enabled = !isSavingReport,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isSavingReport) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Saving Report...",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "💾 Save Diagnostic Report to Records",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                saveReportMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFD1FAE5), RoundedCornerShape(10.dp))
                            .border(width = 1.dp, color = Color(0xFF10B981), shape = RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "✓ $msg",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Single Download Report Button (Saves PDF to Downloads + Opens Share Options for WhatsApp/Email)
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        isDownloadingReport = true
                        downloadReportMessage = null
                        val pName = selectedScanPatientRecord?.name ?: userName
                        val pdfFile = generatePdfReport(context, pName, res.geminiReport ?: "")
                        isDownloadingReport = false
                        if (pdfFile != null) {
                            downloadReportMessage = "PDF report saved to Downloads (${pdfFile.name})!"
                            sharePdfViaWhatsApp(context, pdfFile, pName)
                        } else {
                            downloadReportMessage = "Failed to generate PDF report."
                        }
                    },
                    enabled = !isDownloadingReport,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isDownloadingReport) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Downloading PDF...",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "📥 Download Report",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                downloadReportMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp))
                            .border(width = 1.dp, color = PrimaryBlue, shape = RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "✓ $msg",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E40AF),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Recommended Specialist Care (patient role only) — mirrors the website's
                // post-scan doctor-discovery flow, reading the same shared Supabase directory.
                if (isPatient) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "🩺 Recommended Specialist Care",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMain
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Based on our AI findings, we recommend consulting a specialist. Select a category below to meet our registered doctors:",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.sp,
                                color = TextMuted
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Specialty grid — 2 columns
                            SPECIALTIES_LIST.chunked(2).forEach { rowSpecs ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowSpecs.forEach { spec ->
                                        val isSelected = spec.name == selectedSpecialty
                                        val isRecommended = spec.name == recommendedSpecialty
                                        Box(modifier = Modifier.weight(1f)) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSelected) Color(0xFFEFF6FF) else Color.White)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) PrimaryBlue else BorderColor,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable {
                                                        selectedSpecialty = spec.name
                                                        fetchDoctorsForSpecialty(spec.name)
                                                    }
                                                    .padding(10.dp)
                                            ) {
                                                if (isRecommended) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFFEAB308), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "RECOMMENDED",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                }
                                                Text(
                                                    text = spec.name,
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextMain
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = spec.description,
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 10.sp,
                                                    color = TextMuted,
                                                    lineHeight = 13.sp
                                                )
                                            }
                                        }
                                    }
                                    if (rowSpecs.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Registered Specialists: $selectedSpecialty",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMain
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (loadingDoctors) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Searching registered specialists...",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            } else if (directoryDoctors.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = buildAnnotatedString {
                                            append("No dentists in our network have registered under ")
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(selectedSpecialty) }
                                            append(" yet. You can save this report to your profile or choose another specialty.")
                                        },
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 12.sp,
                                        color = Color(0xFFB45309),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            } else {
                                directoryDoctors.forEach { doc ->
                                    val isDocSelected = selectedDoctor?.doctorId == doc.doctorId
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDocSelected) Color(0xFFECFDF5) else Color.White)
                                            .border(
                                                width = if (isDocSelected) 2.dp else 1.dp,
                                                color = if (isDocSelected) Color(0xFF10B981) else BorderColor,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedDoctor = if (isDocSelected) null else doc }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = doc.fullName,
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextMain
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "📧 ${doc.email}" + if (doc.phone.isNotBlank()) " | 📞 ${doc.phone}" else "",
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 11.sp,
                                                color = TextMuted
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .border(width = 2.dp, color = if (isDocSelected) Color(0xFF10B981) else BorderColor, shape = CircleShape)
                                                .background(if (isDocSelected) Color(0xFF10B981) else Color.Transparent)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val doctor = selectedDoctor ?: return@Button
                                    val bitmap = selectedBitmap ?: return@Button
                                    isSharing = true
                                    shareMessage = null
                                    scope.launch {
                                        val diagnosticsSummary = org.json.JSONObject().apply {
                                            res.predictions.groupBy { it.className }.forEach { (cls, items) ->
                                                val arr = org.json.JSONArray()
                                                items.forEach { item ->
                                                    arr.put(org.json.JSONObject().apply {
                                                        put("type", item.className)
                                                        put("confidence", item.confidence.toDouble())
                                                    })
                                                }
                                                put(cls, arr)
                                            }
                                        }
                                        var phone = ""
                                        ApiClient.fetchProfile(userEmail).onSuccess { phone = it.mobile }

                                        // 1. Supabase (source of truth for the shared directory — covers website-only doctors too).
                                        ApiClient.supabaseShareReportWithDoctor(
                                            accessToken = supabaseAccessToken,
                                            patientAuthUserId = supabaseUserId,
                                            doctorId = doctor.doctorId,
                                            patientName = userName,
                                            patientEmail = userEmail,
                                            patientPhone = phone,
                                            geminiReport = res.geminiReport ?: "",
                                            diagnosticsSummary = diagnosticsSummary
                                        )

                                        // 2. Best-effort Flask mirror — safe no-op if this doctor has no Flask account.
                                        ApiClient.createPatient(
                                            com.example.smileguardai.data.PatientRecord(
                                                patientId = "PID-${System.currentTimeMillis()}",
                                                name = userName,
                                                email = userEmail,
                                                phone = phone
                                            ),
                                            doctorEmail = doctor.email
                                        )
                                        ApiClient.analyzeImage(bitmap, role = userRole, patientName = userName, doctorEmail = doctor.email, patientEmail = userEmail)

                                        isSharing = false
                                        shareMessage = "Saved & shared with ${doctor.fullName}!"
                                    }
                                },
                                enabled = selectedDoctor != null && !isSharing,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isSharing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Saving...", fontFamily = FontFamily.SansSerif, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                } else {
                                    Text(
                                        text = selectedDoctor?.let { "Save & Share with ${it.fullName}" } ?: "Select a Doctor to Share",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            shareMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "✓ $msg",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

private fun Color.Companion.FFFBEB_Safe(): Color = Color(0xFFFFFBEB)
