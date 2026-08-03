package com.example.smileguardai.ui.settings

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smileguardai.R
import com.example.smileguardai.theme.BorderColor
import com.example.smileguardai.theme.PrimaryBlue
import com.example.smileguardai.theme.PrimaryLight
import com.example.smileguardai.theme.TextMain
import com.example.smileguardai.theme.TextMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userName: String = "",
    userEmail: String = "",
    supabaseAccessToken: String = "",
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fullName by remember { mutableStateOf(userName) }
    var email by remember { mutableStateOf(userEmail) }
    var mobile by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var specialization by remember { mutableStateOf("Orthodontics") }
    var address by remember { mutableStateOf("") }

    var genderExpanded by remember { mutableStateOf(false) }
    var specializationExpanded by remember { mutableStateOf(false) }

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var profileSaveMessage by remember { mutableStateOf<String?>(null) }
    var passwordUpdateMessage by remember { mutableStateOf<String?>(null) }
    var isSavingProfile by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Load previously saved profile details for this account, if any
    LaunchedEffect(userEmail) {
        if (userEmail.isNotBlank()) {
            com.example.smileguardai.data.ApiClient.fetchProfile(userEmail).onSuccess { profile ->
                if (profile.name.isNotBlank()) fullName = profile.name
                mobile = profile.mobile
                address = profile.address
                if (profile.gender.isNotBlank()) gender = profile.gender
                if (profile.specialization.isNotBlank()) specialization = profile.specialization
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

        Spacer(modifier = Modifier.height(16.dp))

        // Page Header Title & Subtitle
        Text(
            text = "Settings",
            fontFamily = FontFamily.SansSerif,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Manage your app preferences and account security.",
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Card 1: Personal Information (Matching Screenshot)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header Icon & Title
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(PrimaryLight, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👤", fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Personal Information",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Manage your profile details and contact information.",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = BorderColor, thickness = 1.dp)

                Spacer(modifier = Modifier.height(20.dp))

                // Profile Photo Upload Circle
                Text(
                    text = "Profile Photo",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(width = 1.dp, color = Color(0xFFCBD5E1), shape = CircleShape)
                        .clip(CircleShape)
                        .clickable { /* Select Photo */ },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontSize = 26.sp, color = TextMuted, fontWeight = FontWeight.Normal)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Full Name
                Text(
                    text = "Full Name",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextMain,
                        unfocusedTextColor = TextMain
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email Address
                Text(
                    text = "Email Address",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextMain,
                        unfocusedTextColor = TextMain
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mobile Number
                Text(
                    text = "Mobile Number",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextMain,
                        unfocusedTextColor = TextMain
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Gender Dropdown
                Text(
                    text = "Gender",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Spacer(modifier = Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = !genderExpanded }
                ) {
                    OutlinedTextField(
                        value = gender,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextMain,
                            unfocusedTextColor = TextMain
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        listOf("Male", "Female", "Other").forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextMain
                                    )
                                },
                                onClick = {
                                    gender = option
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Specialization Dropdown (Matching Screenshot)
                Text(
                    text = "Specialization",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Spacer(modifier = Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = specializationExpanded,
                    onExpandedChange = { specializationExpanded = !specializationExpanded }
                ) {
                    OutlinedTextField(
                        value = specialization,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = specializationExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextMain,
                            unfocusedTextColor = TextMain
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = specializationExpanded,
                        onDismissRequest = { specializationExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        listOf(
                            "Orthodontics",
                            "Prosthodontics",
                            "Periodontics",
                            "Endodontics",
                            "Implantology",
                            "Pediatric"
                        ).forEach { specOpt ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = specOpt,
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextMain
                                    )
                                },
                                onClick = {
                                    specialization = specOpt
                                    specializationExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Address
                Text(
                    text = "Address",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextMain,
                        unfocusedTextColor = TextMain
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Save Profile Changes Button
                Button(
                    onClick = {
                        if (userEmail.isBlank()) {
                            profileSaveMessage = "Sign in first to save your profile."
                        } else {
                            scope.launch {
                                isSavingProfile = true
                                val result = com.example.smileguardai.data.ApiClient.saveProfile(
                                    email = userEmail,
                                    name = fullName,
                                    mobile = mobile,
                                    address = address,
                                    gender = gender,
                                    specialization = specialization
                                )
                                isSavingProfile = false
                                profileSaveMessage = if (result.isSuccess && result.getOrDefault(false)) {
                                    "Profile changes saved successfully!"
                                } else {
                                    "Could not save profile: ${result.exceptionOrNull()?.message ?: "unknown error"}"
                                }

                                // Best-effort mirror into Supabase (same project the website uses).
                                // Never overrides the Flask save result above — that's this app's real save.
                                if (supabaseAccessToken.isNotBlank()) {
                                    com.example.smileguardai.data.ApiClient.supabaseUpdateUserMetadata(
                                        supabaseAccessToken,
                                        mapOf(
                                            "full_name" to fullName,
                                            "mobile" to mobile,
                                            "address" to address,
                                            "gender" to gender,
                                            "specialization" to specialization
                                        )
                                    )
                                }
                            }
                        }
                    },
                    enabled = !isSavingProfile,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = if (isSavingProfile) "Saving..." else "Save Profile Changes",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                profileSaveMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✓ $msg",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Card 2: Account Security
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFFEE2E2), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛡️", fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Account Security",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Update your password to keep your account secure.",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = BorderColor, thickness = 1.dp)

                Spacer(modifier = Modifier.height(20.dp))

                // New Password
                Text(
                    text = "New Password",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    placeholder = { Text("Enter new password", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextMain,
                        unfocusedTextColor = TextMain
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Password
                Text(
                    text = "Confirm Password",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = { Text("Re-enter new password", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextMain,
                        unfocusedTextColor = TextMain
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                var isUpdatingPassword by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                Button(
                    onClick = {
                        if (newPassword.isNotBlank() && newPassword == confirmPassword) {
                            scope.launch {
                                isUpdatingPassword = true
                                com.example.smileguardai.data.ApiClient.updatePassword(email, newPassword)
                                // Best-effort mirror into Supabase — Flask's update above is this app's real save.
                                if (supabaseAccessToken.isNotBlank()) {
                                    com.example.smileguardai.data.ApiClient.supabaseUpdatePassword(supabaseAccessToken, newPassword)
                                }
                                isUpdatingPassword = false
                                passwordUpdateMessage = "Password updated successfully in database!"
                                newPassword = ""
                                confirmPassword = ""
                            }
                        } else if (newPassword != confirmPassword) {
                            passwordUpdateMessage = "Passwords do not match."
                        } else {
                            passwordUpdateMessage = "Please enter a new password."
                        }
                    },
                    enabled = !isUpdatingPassword,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = if (isUpdatingPassword) "Updating in DB..." else "Update Password",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                passwordUpdateMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✓ $msg",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }


        Spacer(modifier = Modifier.height(28.dp))

        // Card 3: Sign Out
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = Color(0xFFFEE2E2), shape = RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFFEE2E2), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🚪", fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Sign Out",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Securely log out of your account on this device.",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = BorderColor, thickness = 1.dp)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = "Sign Out",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
