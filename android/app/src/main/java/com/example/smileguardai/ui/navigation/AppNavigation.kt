package com.example.smileguardai.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smileguardai.theme.PrimaryBlue
import com.example.smileguardai.theme.PrimaryLight
import com.example.smileguardai.theme.TextMuted
import com.example.smileguardai.ui.analysis.UploadAnalysisScreen
import com.example.smileguardai.ui.chat.AIChatScreen
import com.example.smileguardai.ui.main.MainScreen
import com.example.smileguardai.ui.onboarding.*
import com.example.smileguardai.ui.patients.PatientsScreen
import com.example.smileguardai.ui.reports.ReportsScreen
import com.example.smileguardai.ui.settings.SettingsScreen
import com.example.smileguardai.ui.treatment.TreatmentPlanScreen

enum class AppDestination {
    SPLASH,
    WELCOME,
    ROLE,
    AUTH,
    DASHBOARD,
    PATIENTS,
    SCAN,
    TREATMENT,
    REPORTS,
    SETTINGS,
    CHAT
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    var currentDestination by remember { mutableStateOf(AppDestination.SPLASH) }
    var userRole by remember { mutableStateOf("doctor") }
    var userName by remember { mutableStateOf("Dr. Dinesh") }
    var userEmail by remember { mutableStateOf("") }
    var supabaseAccessToken by remember { mutableStateOf("") }
    var supabaseUserId by remember { mutableStateOf("") }

    val navBarColors = NavigationBarItemDefaults.colors(
        selectedIconColor = PrimaryBlue,
        selectedTextColor = PrimaryBlue,
        indicatorColor = PrimaryLight,
        unselectedIconColor = TextMuted,
        unselectedTextColor = TextMuted
    )

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (currentDestination in listOf(
                        AppDestination.DASHBOARD,
                        AppDestination.PATIENTS,
                        AppDestination.SCAN,
                        AppDestination.TREATMENT,
                        AppDestination.REPORTS,
                        AppDestination.SETTINGS
                    )
                ) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentDestination == AppDestination.DASHBOARD,
                            onClick = { currentDestination = AppDestination.DASHBOARD },
                            icon = { Text("🏠", fontSize = 18.sp) },
                            label = {
                                Text(
                                    text = "Dashboard",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 9.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (currentDestination == AppDestination.DASHBOARD) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = navBarColors
                        )
                        if (userRole == "doctor") {
                            NavigationBarItem(
                                selected = currentDestination == AppDestination.PATIENTS,
                                onClick = { currentDestination = AppDestination.PATIENTS },
                                icon = { Text("👥", fontSize = 18.sp) },
                                label = {
                                    Text(
                                        text = "Patients",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 9.5.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (currentDestination == AppDestination.PATIENTS) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = navBarColors
                            )
                        }
                        NavigationBarItem(
                            selected = currentDestination == AppDestination.SCAN,
                            onClick = { currentDestination = AppDestination.SCAN },
                            icon = { Text("📷", fontSize = 18.sp) },
                            label = {
                                Text(
                                    text = "Scan",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 9.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (currentDestination == AppDestination.SCAN) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = navBarColors
                        )
                        NavigationBarItem(
                            selected = currentDestination == AppDestination.TREATMENT,
                            onClick = { currentDestination = AppDestination.TREATMENT },
                            icon = { Text("📋", fontSize = 18.sp) },
                            label = {
                                Text(
                                    text = "Treatment",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 9.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (currentDestination == AppDestination.TREATMENT) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = navBarColors
                        )
                        NavigationBarItem(
                            selected = currentDestination == AppDestination.REPORTS,
                            onClick = { currentDestination = AppDestination.REPORTS },
                            icon = { Text("📑", fontSize = 18.sp) },
                            label = {
                                Text(
                                    text = "Reports",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 9.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (currentDestination == AppDestination.REPORTS) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = navBarColors
                        )
                        NavigationBarItem(
                            selected = currentDestination == AppDestination.SETTINGS,
                            onClick = { currentDestination = AppDestination.SETTINGS },
                            icon = { Text("⚙️", fontSize = 18.sp) },
                            label = {
                                Text(
                                    text = "Settings",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 9.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (currentDestination == AppDestination.SETTINGS) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = navBarColors
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentDestination) {
                    AppDestination.SPLASH -> {
                        SplashScreen(onSplashFinished = { currentDestination = AppDestination.WELCOME })
                    }
                    AppDestination.WELCOME -> {
                        WelcomeScreen(onGetStarted = { currentDestination = AppDestination.ROLE })
                    }
                    AppDestination.ROLE -> {
                        RoleSelectionScreen(onRoleSelected = { role ->
                            userRole = role
                            currentDestination = AppDestination.AUTH
                        })
                    }
                    AppDestination.AUTH -> {
                        AuthScreen(
                            role = userRole,
                            onAuthSuccess = { name, email, role, supabaseToken, supabaseUid ->
                                userName = name
                                userEmail = email
                                userRole = role
                                supabaseAccessToken = supabaseToken
                                supabaseUserId = supabaseUid
                                currentDestination = AppDestination.DASHBOARD
                            }
                        )
                    }
                    AppDestination.DASHBOARD -> {
                        MainScreen(
                            userRole = userRole,
                            userName = userName,
                            userEmail = userEmail,
                            supabaseAccessToken = supabaseAccessToken,
                            supabaseUserId = supabaseUserId,
                            onNavigateToScan = { currentDestination = AppDestination.SCAN },
                            onNavigateToChat = { currentDestination = AppDestination.CHAT }
                        )
                    }
                    AppDestination.PATIENTS -> {
                        PatientsScreen(
                            doctorEmail = userEmail,
                            onNavigateBack = { currentDestination = AppDestination.DASHBOARD }
                        )
                    }
                    AppDestination.SCAN -> {
                        UploadAnalysisScreen(
                            userRole = userRole,
                            userName = userName,
                            userEmail = userEmail,
                            supabaseAccessToken = supabaseAccessToken,
                            supabaseUserId = supabaseUserId,
                            onNavigateBack = { currentDestination = AppDestination.DASHBOARD }
                        )
                    }
                    AppDestination.TREATMENT -> {
                        TreatmentPlanScreen(
                            doctorEmail = userEmail,
                            userRole = userRole,
                            onNavigateBack = { currentDestination = AppDestination.DASHBOARD }
                        )
                    }
                    AppDestination.REPORTS -> {
                        ReportsScreen(
                            doctorEmail = userEmail,
                            onNavigateBack = { currentDestination = AppDestination.DASHBOARD }
                        )
                    }
                    AppDestination.SETTINGS -> {
                        SettingsScreen(
                            userName = userName,
                            userEmail = userEmail,
                            supabaseAccessToken = supabaseAccessToken,
                            onNavigateBack = { currentDestination = AppDestination.DASHBOARD },
                            onSignOut = {
                                userEmail = ""
                                userName = if (userRole == "doctor") "Dr. Dinesh" else "Patient"
                                supabaseAccessToken = ""
                                supabaseUserId = ""
                                currentDestination = AppDestination.WELCOME
                            }
                        )
                    }
                    AppDestination.CHAT -> {
                        AIChatScreen(onNavigateBack = { currentDestination = AppDestination.DASHBOARD })
                    }
                }
            }
        }

        // Compact Floating Action Button for SmileGuard AI Assistant
        if (currentDestination in listOf(
                AppDestination.DASHBOARD,
                AppDestination.PATIENTS,
                AppDestination.SCAN,
                AppDestination.TREATMENT,
                AppDestination.REPORTS,
                AppDestination.SETTINGS
            )
        ) {
            FloatingActionButton(
                onClick = { currentDestination = AppDestination.CHAT },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text("💬", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
