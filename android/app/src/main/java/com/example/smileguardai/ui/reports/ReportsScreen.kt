package com.example.smileguardai.ui.reports

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
import androidx.compose.ui.text.style.TextAlign
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

data class ToothFinding(
    val toothNumber: String,
    val tags: List<Pair<String, Int>>
)

data class SavedReportItem(
    val id: String,
    val title: String,
    val date: String,
    val time: String,
    val excerpt: String,
    val concordance: String,
    val toothFindings: List<ToothFinding>,
    val fullReport: String = ""
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    doctorEmail: String = "",
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    var allReports by remember { mutableStateOf(emptyList<SavedReportItem>()) }
    var selectedReport by remember { mutableStateOf<SavedReportItem?>(null) } // null by default -> shows only report cards list until touched!

    // Fetch live reports dynamically from backend, scoped to the signed-in doctor
    LaunchedEffect(doctorEmail) {
        scope.launch {
            ApiClient.fetchDashboardMetrics(doctorEmail).onSuccess { metrics ->
                val fetched = metrics.recentScans.map { r ->
                    SavedReportItem(
                        id = r.id,
                        title = "Scan Report - ${r.patientName}",
                        date = r.date,
                        time = "10:00:00",
                        excerpt = r.summary,
                        concordance = "${r.confidence}%",
                        toothFindings = listOf(
                            ToothFinding("Tooth Analysis:", r.findings.split(',').map { it.trim() to r.confidence })
                        ),
                        fullReport = r.summary
                    )
                }
                allReports = fetched
            }
        }
    }

    val filteredReports = remember(searchQuery, allReports) {
        if (searchQuery.isBlank()) allReports
        else allReports.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.date.contains(searchQuery, ignoreCase = true) ||
            it.excerpt.contains(searchQuery, ignoreCase = true)
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

        // VIEW MODE 1: IF A REPORT IS TOUCHED/OPENED -> SHOW FULL REPORT VIEW
        if (selectedReport != null) {
            val report = selectedReport!!

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { selectedReport = null },
                    modifier = Modifier.size(36.dp).offset(x = (-8).dp)
                ) {
                    Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextMain)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.title,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Recorded on ${report.date} at ${report.time}",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Diagnostic Summary Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Diagnostic Summary",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .background(Color(0xFFD1FAE5), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Concordance: ${report.concordance}",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF059669)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tabular Anomalies Detected Table
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

                    report.toothFindings.forEach { finding ->
                        finding.tags.forEach { (tagLabel, tagConf) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tagLabel.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                    modifier = Modifier.weight(1f),
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMain
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "$tagLabel - $tagConf%",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706),
                                        textAlign = TextAlign.End
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
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                    val fullText = report.fullReport.ifEmpty { report.excerpt }
                    com.example.smileguardai.ui.analysis.MarkdownReportText(
                        text = fullText,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { selectedReport = null },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text("← Back to Reports List", fontWeight = FontWeight.Bold, color = Color.White)
            }
        } else {
            // VIEW MODE 2: MAIN REPORTS LIST (ONLY LIST OF CARDS IS SHOWN)
            Text(
                text = "My Reports",
                fontFamily = FontFamily.SansSerif,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Review saved AI analyses and diagnostics.",
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by patient name or date...", color = Color(0xFF94A3B8), fontSize = 14.sp) },
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

            if (filteredReports.isEmpty()) {
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
                            .padding(vertical = 40.dp, horizontal = 24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("📄", fontSize = 44.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No AI Scan Reports Yet",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Start a new scan in the Scan tab to generate your first AI report.",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // List of Saved Report Cards (Only shown as cards; touch to open!)
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    filteredReports.forEach { report ->
                        Card(
                            onClick = { selectedReport = report }, // TOUCH TO OPEN REPORT!
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = PrimaryBlue,
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = report.title,
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMain
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📅", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = report.date,
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Clinical AI Analysis Report",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "👉 Touch to open full report & diagnostic findings",
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

        Spacer(modifier = Modifier.height(40.dp))
    }
}
