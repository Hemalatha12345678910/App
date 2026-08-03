package com.example.smileguardai.ui.main

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.smileguardai.R
import com.example.smileguardai.theme.BorderColor
import com.example.smileguardai.theme.PrimaryBlue
import com.example.smileguardai.theme.PrimaryLight
import com.example.smileguardai.theme.TextMain
import com.example.smileguardai.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    userRole: String = "doctor",
    userName: String = "Dr. Dinesh",
    userEmail: String = "",
    supabaseAccessToken: String = "",
    supabaseUserId: String = "",
    onNavigateToScan: () -> Unit,
    onNavigateToChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Holds (patientName, patientEmail) for the appointment currently being scheduled.
    var selectedPatientForSchedule by remember { mutableStateOf<Pair<String, String>?>(null) }
    var scheduleSuccessMessage by remember { mutableStateOf<String?>(null) }

    var totalPatients by remember { mutableStateOf("0") }
    var totalScans by remember { mutableStateOf("0") }
    var concordance by remember { mutableStateOf("0%") }
    var recentScans by remember { mutableStateOf(listOf<com.example.smileguardai.data.ClinicalReport>()) }
    var scheduledAppointments by remember { mutableStateOf(listOf<com.example.smileguardai.data.AppointmentRecord>()) }
    val scope = rememberCoroutineScope()

    fun refreshDashboard() {
        scope.launch {
            com.example.smileguardai.data.ApiClient.fetchDashboardMetrics(userEmail).onSuccess { metrics ->
                totalPatients = metrics.totalPatients.toString()
                totalScans = metrics.totalScans.toString()
                concordance = metrics.concordance
                recentScans = metrics.recentScans
                scheduledAppointments = metrics.scheduledAppointments
            }
        }
    }

    // Fetch Live Dashboard Metrics from Backend, scoped to the signed-in doctor
    LaunchedEffect(userEmail) {
        refreshDashboard()
    }

    // Best-effort: keep this doctor's entry in the shared Supabase directory up to date
    // (mirrors website Dashboard.jsx's syncDoctor(), called once per dashboard load) so
    // patients — on the app or the website — can find them under "Recommended Specialist Care".
    LaunchedEffect(userEmail, supabaseAccessToken, supabaseUserId) {
        if (userRole == "doctor" && userEmail.isNotBlank() && supabaseAccessToken.isNotBlank() && supabaseUserId.isNotBlank()) {
            com.example.smileguardai.data.ApiClient.fetchProfile(userEmail).onSuccess { profile ->
                com.example.smileguardai.data.ApiClient.supabaseSyncDoctorProfile(
                    accessToken = supabaseAccessToken,
                    userId = supabaseUserId,
                    fullName = profile.name.ifBlank { userName },
                    email = userEmail,
                    mobile = profile.mobile,
                    specialization = profile.specialization
                )
            }
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

        Spacer(modifier = Modifier.height(20.dp))

        // Greeting Header (Full Width Layout)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Welcome back, $userName",
                fontFamily = FontFamily.SansSerif,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (userRole == "doctor") "Here is your clinical overview for today." else "Here is your personal dental summary.",
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Primary CTA Button (Full Width for Clean Mobile Alignment)
            Button(
                onClick = onNavigateToScan,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Start New Analysis",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "→",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Metric Cards Stack
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (userRole == "doctor") {
                MetricCard(
                    iconText = "👥",
                    iconBgColor = PrimaryLight,
                    value = totalPatients,
                    label = "Total Patients"
                )
                MetricCard(
                    iconText = "📄",
                    iconBgColor = Color(0xFFFEF3C7),
                    value = totalScans,
                    label = "Total Scans"
                )
                MetricCard(
                    iconText = "📈",
                    iconBgColor = Color(0xFFD1FAE5),
                    value = concordance,
                    label = "Concordance level with expert decision"
                )
            } else {
                MetricCard(
                    iconText = "📄",
                    iconBgColor = PrimaryLight,
                    value = totalScans,
                    label = "Total Scans"
                )
                MetricCard(
                    iconText = "📈",
                    iconBgColor = Color(0xFFE0F2FE),
                    value = recentScans.lastOrNull()?.date ?: "—",
                    label = "Last Checkup"
                )
                MetricCard(
                    iconText = "📑",
                    iconBgColor = Color(0xFFFEF3C7),
                    value = recentScans.size.toString(),
                    label = "Saved Reports"
                )
                MetricCard(
                    iconText = "💚",
                    iconBgColor = Color(0xFFD1FAE5),
                    value = concordance,
                    label = "Concordance level with expert decision"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Recent Scans Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Scans",
                fontFamily = FontFamily.SansSerif,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            OutlinedButton(
                onClick = {},
                shape = RoundedCornerShape(8.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(PrimaryBlue)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "View All",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Scans Container Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                if (recentScans.isEmpty()) {
                    Text(
                        text = "No scans yet — run your first AI diagnostic scan to see it here.",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    recentScans.reversed().forEachIndexed { index, report ->
                        if (userRole == "doctor") {
                            DoctorScanRow(
                                name = report.patientName,
                                email = report.patientEmail,
                                dateTime = report.date,
                                onNavigate = onNavigateToScan,
                                onSchedule = { selectedPatientForSchedule = report.patientName to report.patientEmail }
                            )
                        } else {
                            RecentScanItem(name = report.patientName, dateTime = report.date, onClick = onNavigateToScan)
                        }
                        if (index < recentScans.size - 1) {
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // AI Insights Card (Doctor Specific)
        if (userRole == "doctor") {
            Text(
                text = "AI Insights",
                fontFamily = FontFamily.SansSerif,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Insight 1: Caries trend alert
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFEE2E2), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🛡️", fontSize = 18.sp)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Caries trend alert",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMain
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "15% increase in early-stage caries detection among young adults this month.",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.5.sp,
                                color = TextMuted,
                                lineHeight = 19.sp
                            )
                        }
                    }

                    HorizontalDivider(color = BorderColor, thickness = 1.dp)

                    // Insight 2: System update
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE0F2FE), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📈", fontSize = 18.sp)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "System update",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMain
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "The new plaque detection model is now active. Confidence scores improved by 4%.",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.5.sp,
                                color = TextMuted,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Scheduled Appointments — visible to both roles so a doctor-scheduled appointment
        // actually shows up on the patient's own dashboard, not just the doctor's.
        Text(
            text = if (userRole == "doctor") "All Scheduled Appointments" else "My Appointments",
            fontFamily = FontFamily.SansSerif,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (scheduledAppointments.isEmpty()) {
                    Text(
                        text = "No appointments scheduled yet.",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                } else {
                    scheduledAppointments.forEachIndexed { index, appt ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📅", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (userRole == "doctor") appt.patientName else "Upcoming Appointment",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMain
                                )
                            }
                            if (userRole == "doctor") {
                                Text(
                                    text = "Patient",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${appt.date} at ${appt.time}",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )

                        if (userRole == "doctor" && appt.patientEmail.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = appt.patientEmail,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }

                        if (index < scheduledAppointments.size - 1) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }

                scheduleSuccessMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✓ $msg",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // Schedule Appointment Dialog (Matches Screenshot 100%)
    selectedPatientForSchedule?.let { (patientName, patientEmail) ->
        ScheduleAppointmentDialog(
            patientName = patientName,
            onDismiss = { selectedPatientForSchedule = null },
            onConfirm = { date, time ->
                scope.launch {
                    com.example.smileguardai.data.ApiClient.scheduleAppointment(patientName, date, time, userEmail, patientEmail)
                    scheduleSuccessMessage = "Appointment scheduled for $patientName on $date at $time!"
                    refreshDashboard()
                }
                selectedPatientForSchedule = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleAppointmentDialog(
    patientName: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var dateText by remember { mutableStateOf("19-07-2026") }
    var timeText by remember { mutableStateOf("14:30") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Header Title with Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📅", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Schedule Appointment",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Subtitle with Bold Patient Name
                Text(
                    text = buildAnnotatedString {
                        append("Assign an appointment date and time for ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = TextMain)) {
                            append("$patientName ")
                        }
                        append(". This will be visible on their dashboard.")
                    },
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    color = TextMuted,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Select Date Input
                Text(
                    text = "Select Date",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    placeholder = { Text("dd-mm-yyyy", color = Color(0xFF94A3B8)) },
                    trailingIcon = { Text("📅", fontSize = 16.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextMain,
                        unfocusedTextColor = TextMain,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Select Time Input
                Text(
                    text = "Select Time",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    placeholder = { Text("--:--", color = Color(0xFF94A3B8)) },
                    trailingIcon = { Text("🕒", fontSize = 16.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextMain,
                        unfocusedTextColor = TextMain,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Action Buttons Row (Cancel & Confirm)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(PrimaryBlue)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }

                    Button(
                        onClick = { onConfirm(dateText, timeText) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Confirm",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorScanRow(
    name: String,
    email: String,
    dateTime: String,
    onNavigate: () -> Unit,
    onSchedule: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate() }
            .padding(vertical = 14.dp)
    ) {
        // Top Line: Icon, Name/Email, Timestamp & Arrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(PrimaryLight, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("📄", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (email.isNotBlank()) "$name ($email)" else name,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateTime,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "→",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Line: Badges row with ample spacing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 54.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            OutlinedButton(
                onClick = onSchedule,
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(PrimaryBlue))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📅", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Schedule", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .background(Color(0xFFD1FAE5), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "AI Analyzed",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF059669)
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    iconText: String,
    iconBgColor: Color,
    value: String,
    label: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconBgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(iconText, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun RecentScanItem(
    name: String,
    dateTime: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(PrimaryLight, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("📄", fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontFamily = FontFamily.SansSerif,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateTime,
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .background(Color(0xFFD1FAE5), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "AI Analyzed",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF059669)
                )
            }
        }

        Text(
            text = "→",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted
        )
    }
}
