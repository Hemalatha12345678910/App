package com.example.smileguardai.ui.onboarding

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smileguardai.R
import com.example.smileguardai.data.ApiClient
import com.example.smileguardai.theme.BorderColor
import com.example.smileguardai.theme.PrimaryBlue
import com.example.smileguardai.theme.TextMain
import com.example.smileguardai.theme.TextMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    role: String,
    onAuthSuccess: (name: String, email: String, role: String, supabaseAccessToken: String, supabaseUserId: String) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isSignUp by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Form fields
    var fullName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var genderExpanded by remember { mutableStateOf(false) }

    var specialization by remember { mutableStateOf("Orthodontics") }
    var specializationExpanded by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    val portalTitle = if (role == "doctor") "Clinical Portal" else "Patient Portal"
    val defaultNamePlaceholder = if (role == "doctor") "Dr. Jane Doe" else "Jane Doe"

    val specializations = listOf(
        "Orthodontics",
        "Prosthodontics",
        "Periodontics",
        "Endodontics",
        "Implantology",
        "Pediatric"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Back Button (For Sign In)
            if (!isSignUp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Text(
                            text = "←",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Circular Logo
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome Back",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = portalTitle,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 15.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(28.dp))
            } else {
                // Header for Sign Up
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = portalTitle,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Profile Photo Optional Circle
                Text(
                    text = "Profile Photo",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .border(width = 1.dp, color = BorderColor, shape = CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFFFAFAFA))
                        .clickable { /* Select profile photo */ },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Optional",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Form Fields
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isSignUp) {
                    // Full Name
                    FormInputGroup(
                        label = "Full Name",
                        value = fullName,
                        onValueChange = { fullName = it },
                        placeholder = defaultNamePlaceholder
                    )

                    // Mobile Number
                    FormInputGroup(
                        label = "Mobile Number",
                        value = mobileNumber,
                        onValueChange = { mobileNumber = it },
                        placeholder = "+1 (555) 000-0000"
                    )

                    // Address
                    FormInputGroup(
                        label = "Address",
                        value = address,
                        onValueChange = { address = it },
                        placeholder = "123 Clinical Way, NY"
                    )

                    // Gender Dropdown
                    Column {
                        Text(
                            text = "Gender",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
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
                                trailingIcon = {
                                    Text("▼", fontSize = 12.sp, color = TextMuted)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextMain,
                                    unfocusedTextColor = TextMain,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = BorderColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = genderExpanded,
                                onDismissRequest = { genderExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                listOf("Male", "Female", "Other").forEach { item ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = item,
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextMain
                                            )
                                        },
                                        onClick = {
                                            gender = item
                                            genderExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Specialization Dropdown (Only for Clinical / Doctor Portal)
                    if (role == "doctor") {
                        Column {
                            Text(
                                text = "Specialization",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
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
                                    trailingIcon = {
                                        Text("▼", fontSize = 12.sp, color = TextMuted)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextMain,
                                        unfocusedTextColor = TextMain,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = PrimaryBlue,
                                        unfocusedBorderColor = BorderColor
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = specializationExpanded,
                                    onDismissRequest = { specializationExpanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    specializations.forEach { item ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = item,
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextMain
                                                )
                                            },
                                            onClick = {
                                                specialization = item
                                                specializationExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Email Address
                FormInputGroup(
                    label = "Email Address",
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "jane.doe@example.com"
                )

                // Password
                Column {
                    Text(
                        text = "Password",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMain
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("••••••••", color = Color.Gray) },
                        visualTransformation = PasswordVisualTransformation(),
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
                }
            }

            authError?.let { err ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = err,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    color = Color(0xFFDC2626),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Primary Action Button
            Button(
                onClick = {
                    val trimmedEmail = email.trim().lowercase()
                    if (trimmedEmail.isBlank() || password.isBlank()) {
                        authError = "Email and password are required."
                        return@Button
                    }
                    val nameToPass = if (fullName.isNotBlank()) fullName else if (role == "doctor") "Dr. Jane Doe" else "Jane Doe"
                    isSubmitting = true
                    authError = null
                    scope.launch {
                        val result = if (isSignUp) {
                            ApiClient.register(trimmedEmail, password, nameToPass, role)
                        } else {
                            ApiClient.login(trimmedEmail, password, role)
                        }
                        isSubmitting = false
                        result.onSuccess { account ->
                            // The sign-up form collects mobile/address/gender/specialization, but
                            // /api/auth/register only persists email/name/role/password — save the
                            // rest now so Settings (and, for doctors, the specialist directory sync)
                            // reflect what was actually entered instead of silent defaults.
                            if (isSignUp) {
                                ApiClient.saveProfile(
                                    email = trimmedEmail,
                                    name = nameToPass,
                                    mobile = mobileNumber,
                                    address = address,
                                    gender = gender,
                                    specialization = specialization,
                                    role = role
                                )
                            }

                            // Best-effort: mirror this identity into the same Supabase project the
                            // website uses, so Settings can sync there too. Never blocks the app's
                            // own (Flask-backed) login/registration on failure.
                            val supabaseResult = if (isSignUp) {
                                ApiClient.supabaseSignUp(
                                    trimmedEmail,
                                    password,
                                    mapOf("full_name" to account.name, "role" to account.role)
                                )
                            } else {
                                ApiClient.supabaseSignIn(trimmedEmail, password)
                            }
                            val supabaseToken = supabaseResult.getOrNull()?.accessToken ?: ""
                            val supabaseUserId = supabaseResult.getOrNull()?.userId ?: ""
                            onAuthSuccess(account.name, account.email, account.role, supabaseToken, supabaseUserId)
                        }.onFailure { e ->
                            authError = e.message ?: "Something went wrong. Please try again."
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (isSignUp) "Sign Up" else "Sign In",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Footer Switch Link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSignUp) "Already have an account? " else "Don't have an account? ",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    color = TextMuted
                )
                Text(
                    text = if (isSignUp) "Sign in here" else "Sign up here",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    modifier = Modifier.clickable { isSignUp = !isSignUp; authError = null }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun FormInputGroup(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column {
        Text(
            text = label,
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMain
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF94A3B8)) },
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
    }
}
