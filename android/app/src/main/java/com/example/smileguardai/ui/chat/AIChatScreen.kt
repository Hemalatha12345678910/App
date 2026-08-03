package com.example.smileguardai.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smileguardai.data.ApiClient
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String, // "user" or "bot"
    val text: String,
    val time: String = ""
)

@Composable
fun AIChatScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("patient") }

    val suggestions = if (selectedRole == "doctor") {
        listOf(
            "How do I interpret a periapical radiograph?",
            "What are signs of early caries?",
            "Treatment options for class II malocclusion?"
        )
    } else {
        listOf(
            "How often should I brush my teeth?",
            "Why do my gums bleed when flossing?",
            "How can I whiten my teeth safely?"
        )
    }

    fun sendMessage(msgText: String) {
        val userMsg = ChatMessage("user", msgText)
        val updatedList = messages + userMsg
        messages = updatedList
        inputText = ""
        isLoading = true

        scope.launch {
            val historyPayload = updatedList.map { mapOf("sender" to it.sender, "text" to it.text) }
            val result = ApiClient.sendChatMessage(msgText, selectedRole, historyPayload)
            isLoading = false
            result.onSuccess {
                messages = messages + ChatMessage("bot", it.reply)
            }.onFailure {
                messages = messages + ChatMessage("bot", "⚠️ Could not connect to AI Assistant: ${it.message}")
            }
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Header
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0)),
                    contentPadding = PaddingValues(8.dp),
                    shape = CircleShape
                ) {
                    Text("← Back", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "SmileGuard AI Assistant 🦷",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = if (selectedRole == "doctor") "Clinical Dental Decision Support" else "Patient Dental Health Guide",
                        fontSize = 12.sp,
                        color = Color(0xFF0284C7)
                    )
                }
            }
        }


        // Messages List
        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🦷", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Hi! I'm SmileGuard AI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        if (selectedRole == "doctor") "Ask me anything about clinical dentistry, caries management, or radiology."
                        else "Ask me any questions about your dental health or oral hygiene!",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        suggestions.forEach { suggestion ->
                            OutlinedButton(
                                onClick = { sendMessage(suggestion) },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7))
                            ) {
                                Text(suggestion, fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages) { msg ->
                        val isUser = msg.sender == "user"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUser) Color(0xFF0284C7) else Color.White
                                ),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = if (isUser) Color.White else Color(0xFF1E293B),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                    if (isLoading) {
                        item {
                            Text(
                                "SmileGuard AI is thinking...",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Text Field Input Row
        Surface(
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask dental question...", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedPlaceholderColor = Color(0xFF94A3B8),
                        unfocusedPlaceholderColor = Color(0xFF94A3B8),
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            sendMessage(inputText.trim())
                        }
                    },
                    enabled = inputText.isNotBlank() && !isLoading,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Text("Send 🚀", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
