package com.example.smileguardai.ui.patients

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smileguardai.R
import com.example.smileguardai.data.ApiClient
import com.example.smileguardai.data.PatientRecord
import com.example.smileguardai.theme.BorderColor
import com.example.smileguardai.theme.PrimaryBlue
import com.example.smileguardai.theme.PrimaryLight
import com.example.smileguardai.theme.TextMain
import com.example.smileguardai.theme.TextMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientsScreen(
    doctorEmail: String = "",
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf("view") } // "view" or "add"
    var searchQuery by remember { mutableStateOf("") }

    var patientsList by remember {
        mutableStateOf(emptyList<PatientRecord>())
    }

    // Dynamic Fetch Patients from Backend, scoped to the signed-in doctor
    LaunchedEffect(doctorEmail) {
        scope.launch {
            ApiClient.fetchPatients(doctorEmail).onSuccess { fetched ->
                patientsList = fetched
            }
        }
    }

    // Add Patient Form State
    var newPatientId by remember { mutableStateOf("PID-2026-${(1000..9999).random()}") }
    var newFullName by remember { mutableStateOf("") }
    var newDob by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newNotes by remember { mutableStateOf("") }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

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

        // Page Header (Full Width)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Patient Management",
                fontFamily = FontFamily.SansSerif,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "View your patient roster or register a new patient.",
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Full Width Segmented Toggle Pill Container (View Patients | Add Patient)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                    .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(24.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View Patients Pill (50% Width)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (activeTab == "view") PrimaryLight else Color.Transparent)
                        .clickable { activeTab = "view" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👥", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "View Patients",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp,
                            fontWeight = if (activeTab == "view") FontWeight.Bold else FontWeight.Medium,
                            color = if (activeTab == "view") PrimaryBlue else TextMuted
                        )
                    }
                }

                // Add Patient Pill (50% Width)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (activeTab == "add") PrimaryBlue else Color.Transparent)
                        .clickable { activeTab = "add" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👤+", fontSize = 13.sp, color = if (activeTab == "add") Color.White else TextMuted)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Patient",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp,
                            fontWeight = if (activeTab == "add") FontWeight.Bold else FontWeight.Medium,
                            color = if (activeTab == "add") Color.White else TextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (activeTab == "view") {
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search patients by name or email...", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                leadingIcon = { Text("🔍", fontSize = 16.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
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

            Spacer(modifier = Modifier.height(24.dp))

            // Patients Tabular Data Table Card
            val filteredList = patientsList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true) ||
                it.patientId.contains(searchQuery, ignoreCase = true)
            }

            if (filteredList.isEmpty()) {
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
                            .padding(vertical = 36.dp, horizontal = 20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("👥", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Patients Registered Yet",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Click 'Add Patient' above to register your first patient.",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    }
                }
            } else {
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
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        // Table Header Row
                        Row(
                            modifier = Modifier
                                .background(Color(0xFFF8FAFC))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PATIENT ID",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                modifier = Modifier.width(230.dp)
                            )
                            Text(
                                text = "PATIENT NAME",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                modifier = Modifier.width(150.dp)
                            )
                            Text(
                                text = "DOB",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                modifier = Modifier.width(110.dp)
                            )
                            Text(
                                text = "EMAIL",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                modifier = Modifier.width(260.dp)
                            )
                            Text(
                                text = "STATUS",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                modifier = Modifier.width(90.dp)
                            )
                        }

                        HorizontalDivider(color = BorderColor, thickness = 1.dp)

                        // Table Data Rows
                        filteredList.forEachIndexed { index, patient ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = patient.patientId,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue,
                                    maxLines = 2,
                                    softWrap = true,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.width(230.dp)
                                )

                                Text(
                                    text = patient.name,
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMain,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(150.dp)
                                )

                                Text(
                                    text = patient.dob,
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.sp,
                                    color = TextMain,
                                    maxLines = 1,
                                    modifier = Modifier.width(110.dp)
                                )

                                Text(
                                    text = patient.email,
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.sp,
                                    color = TextMain,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(260.dp)
                                )

                                Box(modifier = Modifier.width(90.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFD1FAE5), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = patient.status,
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                }
                            }

                            if (index < filteredList.size - 1) {
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        } else {
            // Register New Patient Form
            Text(
                text = "Register New Patient",
                fontFamily = FontFamily.SansSerif,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Form Field 1: Patient ID
            Text(
                text = "Patient ID (auto-generated, editable)",
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = newPatientId,
                onValueChange = { newPatientId = it },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Form Field 2: Full Name
            Text(
                text = "Full Name",
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = newFullName,
                onValueChange = { newFullName = it },
                placeholder = { Text("e.g. Sarah Williams", color = Color(0xFF94A3B8)) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Form Field 3: Date of Birth
            Text(
                text = "Date of Birth",
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = newDob,
                onValueChange = { newDob = it },
                placeholder = { Text("dd-mm-yyyy", color = Color(0xFF94A3B8)) },
                trailingIcon = { Text("📅", fontSize = 16.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Form Field 4: Email Address
            Text(
                text = "Email Address",
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                placeholder = { Text("sarah@example.com", color = Color(0xFF94A3B8)) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Form Field 5: Phone Number
            Text(
                text = "Phone Number",
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = newPhone,
                onValueChange = { newPhone = it },
                placeholder = { Text("(555) 123-4567", color = Color(0xFF94A3B8)) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Form Field 6: Medical History / Notes
            Text(
                text = "Medical History / Notes",
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = newNotes,
                onValueChange = { newNotes = it },
                placeholder = { Text("Any known allergies, past procedures, etc.", color = Color(0xFF94A3B8)) },
                minLines = 3,
                shape = RoundedCornerShape(10.dp),
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

            // Action Buttons (Cancel & Save Patient Record)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { activeTab = "view" },
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(PrimaryBlue)),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))
                var validationError by remember { mutableStateOf<String?>(null) }

                Column {
                    if (validationError != null) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = "⚠️ ${validationError}",
                                color = Color(0xFFDC2626),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (newPatientId.isBlank() || newFullName.isBlank() || newDob.isBlank() || newEmail.isBlank() || newPhone.isBlank()) {
                                validationError = "All fields (Patient ID, Full Name, DOB, Email Address, Phone Number) are mandatory."
                            } else {
                                validationError = null
                                scope.launch {
                                    isSaving = true
                                    val newPatient = PatientRecord(
                                        id = (patientsList.size + 1).toString(),
                                        patientId = newPatientId.trim(),
                                        name = newFullName.trim(),
                                        dob = newDob.trim(),
                                        email = newEmail.trim(),
                                        phone = newPhone.trim(),
                                        status = "Active"
                                    )
                                    ApiClient.createPatient(newPatient, doctorEmail)
                                    patientsList = (patientsList + newPatient).distinctBy { it.patientId }
                                    saveSuccessMessage = "Patient '${newFullName.trim()}' created successfully!"
                                    isSaving = false
                                    activeTab = "view"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isSaving,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(
                            text = if (isSaving) "Saving..." else "Save Patient Record",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            saveSuccessMessage?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "✓ $msg",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    color = Color(0xFF16A34A),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
