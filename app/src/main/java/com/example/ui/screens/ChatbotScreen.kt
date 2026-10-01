package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppViewModel
import com.example.ui.ChatbotMessage
import com.example.ui.Screen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val messages by viewModel.chatbotMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isChatbotLoading.collectAsStateWithLifecycle()

    val predictionResult by viewModel.predictionResult.collectAsStateWithLifecycle()
    val isPredictionLoading by viewModel.isPredictionLoading.collectAsStateWithLifecycle()

    // Handle immediate prediction render inside chatbot if requested
    LaunchedEffect(predictionResult) {
        if (predictionResult != null) {
            // Append prediction results directly into the chat flow
            // to provide a seamless conversational analysis experience
            viewModel.sendMessageToChatbot("Render detailed AI Academic Advisor risk forecast: \n\n$predictionResult")
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SIS AI Academic Advisor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        // Go back to the correct dashboard based on user role
                        val role = viewModel.currentUser.value?.role
                        when (role) {
                            "Admin" -> viewModel.navigateTo(Screen.AdminDashboard)
                            "Faculty" -> viewModel.navigateTo(Screen.FacultyDashboard)
                            "Student" -> viewModel.navigateTo(Screen.StudentDashboard)
                            else -> viewModel.navigateTo(Screen.Home)
                        }
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- Welcome Info Card ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Student Information System Advisor (English & தமிழ்)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Instant real-time answers for internal marks, CGPA, attendance warnings, fee dues, and exam dates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            // --- Quick Actions / Suggestion Questions ---
            if (messages.size <= 2 && !isLoading) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = "Suggested Inquiries / பரிந்துரைக்கப்பட்ட கேள்விகள்:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionBubble(text = "வருகை விவரம் (Attendance)", onClick = {
                            viewModel.sendMessageToChatbot("வணக்கம், மாணவர்களின் தற்போதைய வருகை சதவீதம் மற்றும் வருகை குறைவு விவரங்களை தமிழில் சொல்லுங்கள்.")
                        }, modifier = Modifier.weight(1f))

                        SuggestionBubble(text = "Check CGPA & Marks", onClick = {
                            viewModel.sendMessageToChatbot("What are the semester internal marks and required marks to achieve a 9.0+ CGPA?")
                        }, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionBubble(text = "Fee Payment & Dues", onClick = {
                            viewModel.sendMessageToChatbot("What are the pending fee dues, payment deadline, and no-dues clearance requirements?")
                        }, modifier = Modifier.weight(1f))

                        SuggestionBubble(text = "Exam Schedule & Hall Ticket", onClick = {
                            viewModel.sendMessageToChatbot("When are the Anna University End-Semester examinations scheduled and how to download hall tickets?")
                        }, modifier = Modifier.weight(1f))
                    }
                }
            }

            // --- Chat Messages List ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(message = msg)
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Gemini is reading database...", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            // --- Message Input Bar ---
            Card(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Ask about low attendance, rosters, leaves...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessageToChatbot(textInput)
                                textInput = ""
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessageToChatbot(textInput)
                                textInput = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send message")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatbotMessage) {
    val isUser = message.sender == "User"
    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
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
                containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 9.sp,
                    color = if (isUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SuggestionBubble(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier.padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
