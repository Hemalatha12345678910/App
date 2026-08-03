package com.example.smileguardai.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smileguardai.R
import com.example.smileguardai.theme.BorderColor
import com.example.smileguardai.theme.PrimaryBlue
import com.example.smileguardai.theme.PrimaryLight
import com.example.smileguardai.theme.TextMuted

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            // Small Circular Logo (.small-logo: width 60px)
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Smile Guard AI Logo",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Title (font-size 1.5rem / 24sp, font-weight 600, color #004b87)
            Text(
                text = "How will you be using",
                fontFamily = FontFamily.SansSerif,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryBlue,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Smile Guard?",
                fontFamily = FontFamily.SansSerif,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryBlue,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle (font-size 0.95rem / 15.2sp, color #64748b)
            Text(
                text = "Select your role to customize your experience.",
                fontFamily = FontFamily.SansSerif,
                fontSize = 15.2.sp,
                fontWeight = FontWeight.Normal,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Vertical Card Stack matching website mobile view
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Doctor / Dentist Card
                RoleCardView(
                    title = "Doctor / Dentist",
                    description = "I want to manage patients and run clinical AI analyses.",
                    iconText = "🩺",
                    onClick = { onRoleSelected("doctor") }
                )

                // Patient Card
                RoleCardView(
                    title = "Patient",
                    description = "I want to track my oral health and view my reports.",
                    iconText = "👤",
                    onClick = { onRoleSelected("patient") }
                )
            }
        }
    }
}

@Composable
fun RoleCardView(
    title: String,
    description: String,
    iconText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 28.dp, horizontal = 20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Role Icon Circle (.role-icon: width 64px, background #E6F0FA)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(PrimaryLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(iconText, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card Title (h3: font-size 1.25rem / 20sp, font-weight 600, color #004b87)
            Text(
                text = title,
                fontFamily = FontFamily.SansSerif,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryBlue,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Card Description (p: font-size 0.95rem / 15.2sp, color #64748b, line-height 20sp)
            Text(
                text = description,
                fontFamily = FontFamily.SansSerif,
                fontSize = 15.2.sp,
                fontWeight = FontWeight.Normal,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}
