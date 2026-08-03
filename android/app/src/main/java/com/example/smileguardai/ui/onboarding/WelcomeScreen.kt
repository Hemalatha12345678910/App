package com.example.smileguardai.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.example.smileguardai.theme.PrimaryBlue
import com.example.smileguardai.theme.TextMuted

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
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
            modifier = Modifier.padding(24.dp)
        ) {
            // Circular Logo Emblem (.welcome-logo: width 140px)
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Smile Guard AI Logo",
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main Title (.welcome-content h1: font-size 2rem, color #004b87, font-weight 600)
            Text(
                text = "Welcome to",
                fontFamily = FontFamily.SansSerif,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryBlue,
                textAlign = TextAlign.Center,
                lineHeight = 38.sp
            )
            Text(
                text = "Smile Guard AI",
                fontFamily = FontFamily.SansSerif,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryBlue,
                textAlign = TextAlign.Center,
                lineHeight = 38.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle (.welcome-subtitle: font-size 1.1rem, color #64748b)
            Text(
                text = "Precision Prevention, Powered by AI",
                fontFamily = FontFamily.SansSerif,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(44.dp))

            // Large Pill Button (.btn-large: padding 1rem 2.5rem, font-size 1.1rem, border-radius 2rem, bg #004b87)
            Button(
                onClick = onGetStarted,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(32.dp),
                contentPadding = PaddingValues(horizontal = 40.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Get Started",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 17.6.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "→",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
