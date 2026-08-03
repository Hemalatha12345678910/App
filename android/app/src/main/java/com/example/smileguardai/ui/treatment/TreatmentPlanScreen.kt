package com.example.smileguardai.ui.treatment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smileguardai.R
import com.example.smileguardai.data.ApiClient
import com.example.smileguardai.theme.BorderColor
import com.example.smileguardai.theme.PrimaryBlue
import com.example.smileguardai.theme.PrimaryLight
import com.example.smileguardai.theme.TextMain
import com.example.smileguardai.theme.TextMuted
import kotlinx.coroutines.launch

data class TimelineStage(
    val id: String,
    val title: String,
    var status: String = "Pending",
    var isCompleted: Boolean = false
)

@Composable
fun SectionHeaderItem(
    iconText: String,
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(iconText, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontFamily = FontFamily.SansSerif,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
        }
        Text(
            text = if (isExpanded) "▲" else "▼",
            fontSize = 12.sp,
            color = TextMuted
        )
    }
}

@Composable
fun EditableTimelineStageCard(
    stage: TimelineStage,
    onToggleComplete: () -> Unit,
    readOnly: Boolean = false
) {
    val isCompleted = stage.isCompleted

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFFEEFBF3) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isCompleted) Color(0xFF86EFAC) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(12.dp)
            )
            .let { if (readOnly) it else it.clickable { onToggleComplete() } }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) Color(0xFF22C55E) else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (isCompleted) Color(0xFF22C55E) else Color(0xFFCBD5E1),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = stage.title,
                fontFamily = FontFamily.SansSerif,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatmentPlanScreen(
    doctorEmail: String = "",
    userRole: String = "doctor",
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPatient = userRole == "patient"

    var selectedPatient by remember { mutableStateOf<String?>(null) }
    var isPatientDropdownExpanded by remember { mutableStateOf(false) }

    var patientRecords by remember { mutableStateOf(emptyList<com.example.smileguardai.data.PatientRecord>()) }
    var patientsList by remember { mutableStateOf(emptyList<String>()) }
    var patientLookupFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Doctor: load their full roster for the picker. Patient: resolve their own login email to
    // whichever patientId a doctor already registered them under, and auto-select it (read-only).
    LaunchedEffect(doctorEmail) {
        scope.launch {
            if (isPatient) {
                patientLookupFailed = false
                ApiClient.fetchPatientSelf(doctorEmail).onSuccess { record ->
                    patientRecords = listOf(record)
                    selectedPatient = "${record.name} (${record.patientId})"
                }.onFailure {
                    patientRecords = emptyList()
                    selectedPatient = null
                    patientLookupFailed = true
                }
            } else {
                ApiClient.fetchPatients(doctorEmail).onSuccess { fetched ->
                    patientRecords = fetched
                    patientsList = fetched.map { "${it.name} (${it.patientId})" }
                }
            }
        }
    }

    val selectedPatientRecord = remember(selectedPatient, patientRecords) {
        patientRecords.find { "${it.name} (${it.patientId})" == selectedPatient }
    }

    var stages by remember {
        mutableStateOf(
            listOf(
                TimelineStage("1", "Initial Consultation"),
                TimelineStage("2", "X-rays"),
                TimelineStage("3", "Treatment Planning"),
                TimelineStage("4", "Procedure / Treatment"),
                TimelineStage("5", "Follow-up & Adjustments"),
                TimelineStage("6", "Treatment Completion")
            )
        )
    }
    var clinicalNotesText by remember { mutableStateOf("") }
    var upcomingProceduresText by remember { mutableStateOf("") }
    var planSaveMessage by remember { mutableStateOf<String?>(null) }

    // Load this patient's persisted treatment plan (stages + notes + procedures) whenever selection changes
    LaunchedEffect(selectedPatientRecord?.patientId) {
        val key = selectedPatientRecord?.patientId
        planSaveMessage = null
        if (key != null) {
            ApiClient.fetchTreatmentPlan(key).onSuccess { plan ->
                if (plan.stages.isNotEmpty()) {
                    stages = plan.stages.map { TimelineStage(it.id, it.title, it.status, it.isCompleted) }
                }
                clinicalNotesText = plan.clinicalNotes
                upcomingProceduresText = plan.upcomingProcedures
            }
        }
    }

    // Saves the whole page (timeline + clinical notes + upcoming procedures) as one unit.
    fun persistPlan(updatedStages: List<TimelineStage> = stages) {
        val key = selectedPatientRecord?.patientId ?: return
        scope.launch {
            ApiClient.saveTreatmentPlan(
                key,
                updatedStages.map { com.example.smileguardai.data.TreatmentStageDto(it.id, it.title, it.status, it.isCompleted) },
                clinicalNotesText,
                upcomingProceduresText
            )
        }
    }

    // Past scans for this patient, used by the Scan Comparisons section below
    var patientScans by remember { mutableStateOf(emptyList<com.example.smileguardai.data.ClinicalReport>()) }
    var selectedForCompare by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(selectedPatientRecord?.name, doctorEmail) {
        val name = selectedPatientRecord?.name
        selectedForCompare = emptySet()
        if (name != null) {
            ApiClient.fetchDashboardMetrics(doctorEmail).onSuccess { metrics ->
                patientScans = metrics.recentScans.filter { it.patientName == name }
            }
        } else {
            patientScans = emptyList()
        }
    }

    var newCustomStageName by remember { mutableStateOf("") }
    var uploadedPhotos by remember { mutableStateOf(listOf<Bitmap>()) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmap?.let { b -> uploadedPhotos = uploadedPhotos + b }
            } catch (_: Exception) {}
        }
    }

    var timelineExpanded by remember { mutableStateOf(true) }
    var notesExpanded by remember { mutableStateOf(true) }
    var photosExpanded by remember { mutableStateOf(true) }
    var proceduresExpanded by remember { mutableStateOf(true) }
    var comparisonsExpanded by remember { mutableStateOf(true) }

    val completedCount = stages.count { it.isCompleted || it.status == "Completed" }
    val progressPercent = if (stages.isNotEmpty()) completedCount.toFloat() / stages.size.toFloat() else 0f

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

        Text(
            text = if (isPatient) "My Treatment Plan" else "Treatment Plans",
            fontFamily = FontFamily.SansSerif,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isPatient) "Your care timeline as set by your dentist." else "Create and manage patient treatment timelines.",
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!isPatient) {
        Text(
            text = "Select Patient",
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedCard(
                onClick = { isPatientDropdownExpanded = true },
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
                        text = selectedPatient ?: "-- Choose a patient --",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        fontWeight = if (selectedPatient != null) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedPatient != null) TextMain else TextMuted,
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
                expanded = isPatientDropdownExpanded,
                onDismissRequest = { isPatientDropdownExpanded = false },
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
                            color = if (selectedPatient == null) Color.White else TextMain
                        )
                    },
                    onClick = {
                        selectedPatient = null
                        isPatientDropdownExpanded = false
                    },
                    modifier = Modifier.background(if (selectedPatient == null) Color(0xFF1D4ED8) else Color.White)
                )

                patientsList.forEach { patientName ->
                    val isSelected = selectedPatient == patientName
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = patientName,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextMain
                            )
                        },
                        onClick = {
                            selectedPatient = patientName
                            isPatientDropdownExpanded = false
                        },
                        modifier = Modifier.background(if (isSelected) Color(0xFF1D4ED8) else Color.White)
                    )
                }
            }
        }
        } // !isPatient

        Spacer(modifier = Modifier.height(28.dp))

        if (selectedPatient == null) {
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
                    Text(if (isPatient) "🦷" else "🔒", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isPatient) "No Treatment Plan Yet" else "Patient Selection Required",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPatient) "Your dentist hasn't set up a treatment plan for you yet. Check back after your first visit." else "Please select a patient from the dropdown above to view, edit, or approve their treatment plan and progress photos.",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = Color(0xFFB45309),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Overall Progress",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Text(
                    text = "${(progressPercent * 100).toInt()}%",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progressPercent },
                color = PrimaryBlue,
                trackColor = Color(0xFFE2E8F0),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$completedCount of ${stages.size} stages completed",
                fontFamily = FontFamily.SansSerif,
                fontSize = 13.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(28.dp))

            SectionHeaderItem(
                iconText = "🕒",
                title = "Treatment Timeline",
                isExpanded = timelineExpanded,
                onToggle = { timelineExpanded = !timelineExpanded }
            )

            AnimatedVisibility(visible = timelineExpanded) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    stages.forEach { stage ->
                        EditableTimelineStageCard(
                            stage = stage,
                            readOnly = isPatient,
                            onToggleComplete = {
                                stages = stages.map {
                                    if (it.id == stage.id) it.copy(
                                        isCompleted = !it.isCompleted,
                                        status = if (!it.isCompleted) "Completed" else "Pending"
                                    ) else it
                                }
                                persistPlan(stages)
                            }
                        )
                    }

                    if (!isPatient) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCustomStageName,
                            onValueChange = { newCustomStageName = it },
                            placeholder = { Text("Add custom stage...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newCustomStageName.isNotBlank()) {
                                    stages = stages + TimelineStage(
                                        id = (stages.size + 1).toString(),
                                        title = newCustomStageName.trim()
                                    )
                                    persistPlan(stages)
                                    newCustomStageName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+ Add", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    } // !isPatient
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            SectionHeaderItem(
                iconText = "📝",
                title = "Clinical Notes",
                isExpanded = notesExpanded,
                onToggle = { notesExpanded = !notesExpanded }
            )

            AnimatedVisibility(visible = notesExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    OutlinedTextField(
                        value = clinicalNotesText,
                        onValueChange = { clinicalNotesText = it },
                        readOnly = isPatient,
                        placeholder = { Text(if (isPatient) "Your dentist hasn't added any notes yet." else "Enter patient clinical notes, observations, or diagnosis...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                        minLines = 4,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedTextColor = TextMain,
                            focusedTextColor = TextMain
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            SectionHeaderItem(
                iconText = "📅",
                title = "Upcoming Procedures",
                isExpanded = proceduresExpanded,
                onToggle = { proceduresExpanded = !proceduresExpanded }
            )

            AnimatedVisibility(visible = proceduresExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    OutlinedTextField(
                        value = upcomingProceduresText,
                        onValueChange = { upcomingProceduresText = it },
                        readOnly = isPatient,
                        placeholder = { Text(if (isPatient) "No upcoming procedures scheduled yet." else "Enter scheduled upcoming procedures...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                        minLines = 3,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedTextColor = TextMain,
                            focusedTextColor = TextMain
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            SectionHeaderItem(
                iconText = "📸",
                title = "Progress Photos",
                isExpanded = photosExpanded,
                onToggle = { photosExpanded = !photosExpanded }
            )

            AnimatedVisibility(visible = photosExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Button(
                        onClick = { photoLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("+ Upload Progress Photo", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (uploadedPhotos.isEmpty()) {
                        Text(
                            text = "No progress photos uploaded yet.",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            uploadedPhotos.forEach { photoBitmap ->
                                Image(
                                    bitmap = photoBitmap.asImageBitmap(),
                                    contentDescription = "Progress Photo",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            SectionHeaderItem(
                iconText = "📊",
                title = "Scan Comparisons",
                isExpanded = comparisonsExpanded,
                onToggle = { comparisonsExpanded = !comparisonsExpanded }
            )

            AnimatedVisibility(visible = comparisonsExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    if (patientScans.isEmpty()) {
                        Text(
                            text = "No past scans for this patient yet.",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    } else {
                        Text(
                            text = "Select up to 2 scans to compare side by side.",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        patientScans.forEach { report ->
                            val isSelected = selectedForCompare.contains(report.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) PrimaryLight else Color.Transparent)
                                    .clickable {
                                        selectedForCompare = when {
                                            isSelected -> selectedForCompare - report.id
                                            selectedForCompare.size < 2 -> selectedForCompare + report.id
                                            else -> selectedForCompare
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = report.date,
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMain
                                    )
                                    Text(
                                        text = report.findings,
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                                Text(
                                    text = "${report.confidence}%",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        }

                        val chosen = patientScans.filter { selectedForCompare.contains(it.id) }
                        if (chosen.size == 2) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                chosen.forEach { report ->
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = report.date,
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextMain
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = report.findings,
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 12.sp,
                                            color = TextMuted
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${report.confidence}% confidence",
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isPatient) {
            Spacer(modifier = Modifier.height(28.dp))

            // One common save action for the whole page — timeline, clinical notes, and upcoming procedures together.
            Button(
                onClick = {
                    persistPlan()
                    planSaveMessage = "Treatment plan saved!"
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Save Treatment Plan",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            planSaveMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ $msg",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF16A34A)
                )
            }
            } // !isPatient
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
