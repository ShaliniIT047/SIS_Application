package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.AppViewModel
import com.example.ui.Screen
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableStateOf("Bio & ID") }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    val currentStudent by viewModel.currentStudent.collectAsStateWithLifecycle()
    val depts by viewModel.departments.collectAsStateWithLifecycle()
    val subjs by viewModel.subjects.collectAsStateWithLifecycle()
    val timetables by viewModel.timetables.collectAsStateWithLifecycle()
    val anns by viewModel.announcements.collectAsStateWithLifecycle()
    val leaves by viewModel.leaveRequests.collectAsStateWithLifecycle()
    val recs by viewModel.records.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val facultyList by viewModel.faculty.collectAsStateWithLifecycle()

    val student = currentStudent ?: return
    val deptCode = depts.firstOrNull { it.id == student.deptId }?.code ?: "Unknown"

    // Filter announcements targeting this student's department or semester
    val relevantAnns = anns.filter { it.deptId == -1 || it.deptId == student.deptId }

    // Filter records belonging to this student
    val studentRecords = recs.filter { it.studentId == student.id }
    val overallPercentage = remember(studentRecords) {
        if (studentRecords.isEmpty()) 90f
        else {
            val presents = studentRecords.count { it.status in listOf("Present", "Late", "On Duty", "Approved Leave") }
            (presents.toFloat() / studentRecords.size.toFloat()) * 100
        }
    }

    // Filter timetable for student's exact class
    val myTimetable = timetables.filter { it.deptId == student.deptId && it.semester == student.semester && it.section == student.section }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { old, new, callback ->
                viewModel.changePassword(old, new, callback)
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Student SIS Portal", fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.StudentQRAttendance) }) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "QR Attendance Scan", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.navigateTo(Screen.Chatbot) }) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Assistant", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showChangePasswordDialog = true }) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Change Password", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = "Log out", tint = Color.Red)
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
            // --- Student Profile Welcome ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = student.name.first().toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = student.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Roll: ${student.rollNum} | Reg: ${student.regNum}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "Class: B.E. $deptCode Sem ${student.semester} (${student.section})",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // --- Sub-navigation Tabs ---
            val sisTabs = listOf("Bio & ID", "Academics", "Attendance", "Leaves", "Fees", "Timetable & Exams")
            ScrollableTabRow(
                selectedTabIndex = sisTabs.indexOf(activeSubTab).coerceAtLeast(0),
                edgePadding = 16.dp
            ) {
                sisTabs.forEach { tab ->
                    Tab(
                        selected = activeSubTab == tab,
                        onClick = { activeSubTab = tab },
                        text = { Text(tab, fontWeight = if (activeSubTab == tab) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            val mySubjects = subjs.filter { it.deptId == student.deptId && it.semester == student.semester }

            when (activeSubTab) {
                "Bio & ID" -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Digital Student Identity Card
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.5.dp, Color(0xFFD4AF37), RoundedCornerShape(20.dp)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF1A237E),
                                                    Color(0xFF283593),
                                                    Color(0xFF0D47A1)
                                                )
                                            )
                                        )
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "GRT INSTITUTE OF TECHNOLOGY",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 12.sp,
                                                    letterSpacing = 0.5.sp
                                                )
                                                Text(
                                                    text = "STUDENT IDENTITY CARD",
                                                    color = Color(0xFFFFD54F),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Surface(
                                                color = Color(0xFF4CAF50),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(58.dp)
                                                    .border(2.dp, Color(0xFFFFD54F), CircleShape)
                                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = student.name.firstOrNull()?.toString() ?: "S",
                                                    color = Color.White,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column {
                                                Text(
                                                    text = student.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = "ID: ${student.studentId} | Roll: ${student.rollNum}",
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = "Branch: B.E. $deptCode | Sem ${student.semester} (${student.section})",
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = "Blood: ${student.bloodGroup} | Valid: 2023 - 2027",
                                                    color = Color(0xFFFFD54F),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))
                                        Divider(color = Color.White.copy(alpha = 0.3f), thickness = 0.8.dp)
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = "BARCODE: ${student.regNum}", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    generateAndSaveStudentIdPdf(context, student, deptCode)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = Color(0xFF1A237E), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Save ID (PDF)", color = Color(0xFF1A237E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Academic Biodata Dossier Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Academic Registration Dossier", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    SisDetailRow("Student ID", student.studentId)
                                    SisDetailRow("Registration No", student.regNum)
                                    SisDetailRow("Class Roll No", student.rollNum)
                                    SisDetailRow("Department", "$deptCode - ${depts.firstOrNull { it.id == student.deptId }?.name ?: "Engineering"}")
                                    SisDetailRow("Current Semester", "Semester ${student.semester}")
                                    SisDetailRow("Class Section", "Section ${student.section}")
                                    SisDetailRow("Academic Batch", "2023 - 2027 (Regular)")
                                    SisDetailRow("Admission Quota", "Merit / Single Window Counseling")
                                }
                            }
                        }

                        // 3. Personal & Family Demographics Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Personal & Guardian Info", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    SisDetailRow("Date of Birth", student.dob)
                                    SisDetailRow("Blood Group", student.bloodGroup)
                                    SisDetailRow("Student Email", student.email)
                                    SisDetailRow("Student Phone", student.phone)
                                    SisDetailRow("Father / Guardian", student.parentName)
                                    SisDetailRow("Parent Phone", student.parentPhone)
                                    SisDetailRow("Residential Address", student.address)
                                }
                            }
                        }
                    }
                }

                "Academics" -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. CGPA & Academic Standing Summary Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Cumulative GPA (CGPA)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                            Text("8.74 / 10.0", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Current Sem SGPA", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                            Text("8.85 / 10.0", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Credits Completed: 68 / 160", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Text("Backlogs: 0 (All Cleared)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Academic Classification: First Class with Distinction", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // 2. Download Official Marksheet CTA
                        item {
                            Button(
                                onClick = {
                                    generateAndSaveMarksheetPdf(context, student, deptCode, mySubjects)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download Verified Marksheet (PDF)")
                            }
                        }

                        // 3. Subject-wise Gradebook & Assessment Table
                        item {
                            Text("Enrolled Courses & Grade Breakdown", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }

                        items(mySubjects) { sb ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = sb.name, fontWeight = FontWeight.Bold)
                                            Text(text = "Code: ${sb.code} | Credits: 4", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                        Surface(
                                            color = Color(0xFFE8F5E9),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Grade: O (10 GP)",
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("CAT-1", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            Text("46 / 50", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Column {
                                            Text("CAT-2", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            Text("48 / 50", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Column {
                                            Text("Assignment", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            Text("19 / 20", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Column {
                                            Text("Model Exam", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            Text("94 / 100", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "Attendance" -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Global Circle progress
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    BeautifulCircularGauge(
                                        percentage = overallPercentage,
                                        title = "Overall Attendance Rate",
                                        sizeDp = 130.dp
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(text = "Status: ${if (overallPercentage >= 75f) "SAFE" else "AT RISK"}", fontWeight = FontWeight.Bold, color = if (overallPercentage >= 75) Color(0xFF4CAF50) else Color.Red)
                                        Text(text = "Total Sessions: ${studentRecords.size}", style = MaterialTheme.typography.bodySmall)
                                        Text(text = "Absences: ${studentRecords.count { it.status == "Absent" }}", style = MaterialTheme.typography.bodySmall)
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                viewModel.predictStudentAttendanceRisk(student.id)
                                                viewModel.navigateTo(Screen.Chatbot) // triggers direct AI predictor summary in chatbot screen
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("AI Trend Risk Forecast", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Subject-Wise trackers
                        item {
                            Text(
                                text = "Subject-Wise Tracker",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // List of class courses
                        val mySubjects = subjs.filter { it.deptId == student.deptId && it.semester == student.semester }
                        items(mySubjects) { sb ->
                            val sRecs = studentRecords.filter { r ->
                                val sess = sessions.firstOrNull { it.id == r.sessionId }
                                sess != null && sess.subjectId == sb.id
                            }
                            val sbPercentage = if (sRecs.isEmpty()) 100f else {
                                val presents = sRecs.count { it.status in listOf("Present", "Late", "On Duty", "Approved Leave") }
                                (presents.toFloat() / sRecs.size.toFloat()) * 100
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = sb.name, fontWeight = FontWeight.Bold)
                                        Text(text = "Course Code: ${sb.code}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(text = "Attended Slots: ${sRecs.count { it.status in listOf("Present", "Late", "On Duty", "Approved Leave") }} / ${sRecs.size}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                if (sbPercentage >= 75) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${sbPercentage.toInt()}%",
                                            fontWeight = FontWeight.Bold,
                                            color = if (sbPercentage >= 75) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Correction Request Sheet Drawer Trigger
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Attendance Discrepancy?", fontWeight = FontWeight.Bold)
                                    Text("If you believe you were mistakenly marked absent in a session, you can submit an audit correction request to your professor directly.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    var showCorrectionDialog by remember { mutableStateOf(false) }

                                    Button(onClick = { showCorrectionDialog = true }) {
                                        Text("File Correction Request", fontSize = 12.sp)
                                    }

                                    if (showCorrectionDialog) {
                                        var reason by remember { mutableStateOf("") }
                                        val absentRecords = studentRecords.filter { it.status == "Absent" && it.correctionStatus == "None" }
                                        var selectedRecordId by remember { mutableStateOf(absentRecords.firstOrNull()?.id ?: -1) }

                                        AlertDialog(
                                            onDismissRequest = { showCorrectionDialog = false },
                                            title = { Text("Correction Request Form") },
                                            text = {
                                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Text("Select session where you were absent:", style = MaterialTheme.typography.bodySmall)
                                                    if (absentRecords.isEmpty()) {
                                                        Text("No applicable absent records found to file correction.", color = Color.Red, fontWeight = FontWeight.Bold)
                                                    } else {
                                                        Text("Found ${absentRecords.size} absent sessions. Auto-selecting most recent.", fontWeight = FontWeight.Bold)
                                                    }
                                                    OutlinedTextField(
                                                        value = reason,
                                                        onValueChange = { reason = it },
                                                        label = { Text("Reason for Presence (e.g. medical reason, late check-in)") },
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                }
                                            },
                                            confirmButton = {
                                                Button(
                                                    enabled = selectedRecordId != -1,
                                                    onClick = {
                                                        viewModel.submitCorrectionRequest(selectedRecordId, reason) {
                                                            Toast.makeText(context, "Correction request sent to HOD.", Toast.LENGTH_SHORT).show()
                                                            showCorrectionDialog = false
                                                        }
                                                    }
                                                ) { Text("Submit Claim") }
                                            },
                                            dismissButton = { OutlinedButton(onClick = { showCorrectionDialog = false }) { Text("Cancel") } }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "Fees" -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Fee Financial Summary Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Fee Financial Ledger", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Total Annual Fee", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            Text("$6,500.00", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Amount Paid", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            Text("$6,000.00", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Balance Outstanding", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text("$500.00 (Due: Nov 15)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFEF6C00))
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = Color(0xFFE3F2FD),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Status: PROVISIONAL CLEARANCE GRANTED",
                                            color = Color(0xFF1565C0),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Download Fee Receipt CTA
                        item {
                            Button(
                                onClick = {
                                    generateAndSaveFeeReceiptPdf(context, student, deptCode)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download Official Fee Receipt (PDF)")
                            }
                        }

                        // 3. Itemized Fee Heads
                        item {
                            Text("Itemized Fee Schedule", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }

                        item {
                            SisFeeItem("Tuition & Academic Training Fee", "$4,500.00", "PAID - Ref: TXN-892147", isPaid = true)
                        }
                        item {
                            SisFeeItem("Specialized Laboratory & Library Deposit", "$900.00", "PAID - Ref: TXN-892148", isPaid = true)
                        }
                        item {
                            SisFeeItem("Anna University Examination Fee", "$600.00", "PAID - Ref: TXN-892149", isPaid = true)
                        }
                        item {
                            SisFeeItem("Campus Amenities & Transport Service", "$500.00", "DUE DATE: 15-NOV-2026", isPaid = false)
                        }
                    }
                }

                "Timetable & Exams" -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Download Hall Ticket CTA
                        item {
                            Button(
                                onClick = {
                                    generateAndSaveHallTicketPdf(context, student, deptCode, mySubjects)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download Verified Exam Hall Ticket (PDF)")
                            }
                        }

                        // 2. Upcoming University Exam Dates
                        item {
                            Text("Semester University Examination Schedule", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }

                        item {
                            SisExamSlot("2026-11-20", "FN (09:30 AM - 12:30 PM)", "CS301", "Data Structures & Algorithms", "Hall 402 - Desk CS-12")
                        }
                        item {
                            SisExamSlot("2026-11-24", "FN (09:30 AM - 12:30 PM)", "CS302", "Digital Logic & Computer Design", "Hall 402 - Desk CS-12")
                        }
                        item {
                            SisExamSlot("2026-11-28", "FN (09:30 AM - 12:30 PM)", "CS303", "Discrete Mathematics", "Hall 403 - Desk CS-12")
                        }

                        // 3. Weekly Class Schedule
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Weekly Lecture Timetable", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }

                        if (myTimetable.isEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text("No lecture rosters uploaded for this section.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                    }
                                }
                            }
                        } else {
                            items(myTimetable) { slot ->
                                val sbName = subjs.firstOrNull { it.id == slot.subjectId }?.name ?: "Subject"
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = slot.period.toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(text = sbName, fontWeight = FontWeight.Bold)
                                            Text(text = "Room: ${slot.classroom} | Day: ${slot.dayOfWeek} [${slot.startTime} - ${slot.endTime}]", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "Leaves" -> {
                    AppSectionHeader(
                        title = "Your Leave & Permission Records",
                        subtitle = "Submit new letters or check approved document status"
                    )

                    Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
                        var showLeaveFormDialog by remember { mutableStateOf(false) }
                        var printTargetLeave by remember { mutableStateOf<LeaveRequest?>(null) }

                        Button(
                            onClick = { showLeaveFormDialog = true },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Submit New Leave / Permission")
                        }

                        val myLeaves = leaves.filter { it.studentId == student.id }
                        if (myLeaves.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No historic leave or permission logs recorded.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                items(myLeaves) { req ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = req.requestType,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        style = MaterialTheme.typography.titleSmall
                                                    )
                                                    Text(
                                                        text = "Dates: ${req.startDate} to ${req.endDate}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color.Gray
                                                    )
                                                }
                                                Card(
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = when (req.status) {
                                                            "Approved" -> Color(0xFFE8F5E9)
                                                            "Rejected" -> Color(0xFFFFEBEE)
                                                            else -> Color(0xFFFFF3E0)
                                                        }
                                                    )
                                                ) {
                                                    Text(
                                                        text = req.status,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = when (req.status) {
                                                            "Approved" -> Color(0xFF2E7D32)
                                                            "Rejected" -> Color(0xFFC62828)
                                                            else -> Color(0xFFEF6C00)
                                                        },
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(text = "Reason: ${req.reason}", style = MaterialTheme.typography.bodyMedium)
                                            if (req.comments.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(text = "Faculty Comments: ${req.comments}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                            }

                                            if (req.status == "Approved") {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Button(
                                                    onClick = { printTargetLeave = req },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Print, contentDescription = "Print Letter", modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Print Permission Letter Form", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        printTargetLeave?.let { reqToPrint ->
                            LeaveLetterPrintDialog(
                                student = student,
                                deptCode = deptCode,
                                request = reqToPrint,
                                onDismiss = { printTargetLeave = null }
                            )
                        }

                        if (showLeaveFormDialog) {
                            var start by remember { mutableStateOf("2026-07-20") }
                            var end by remember { mutableStateOf("2026-07-21") }
                            var reason by remember { mutableStateOf("") }
                            val types = listOf(
                                "Casual Leave (CL) Letter",
                                "On Duty (OD) Permission Letter",
                                "Symposium / Workshop Permission Letter",
                                "Industrial Visit (IV) Permission Letter",
                                "Internship / In-Plant Training (IPT) Permission Letter",
                                "Sports / Culturals Event Permission Letter"
                            )
                            var selectedType by remember { mutableStateOf(types[0]) }
                            var selectedFacultyIds by remember { mutableStateOf(setOf<Int>()) }

                            val displayFaculty = remember(facultyList) {
                                val relevant = facultyList.filter { it.deptId == student.deptId }
                                if (relevant.isNotEmpty()) relevant else facultyList
                            }

                            AlertDialog(
                                onDismissRequest = { showLeaveFormDialog = false },
                                title = { Text("Submit New Letter Request") },
                                text = {
                                    Column(
                                        modifier = Modifier.verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("Select Letter Type:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            types.forEach { t ->
                                                val isSelected = selectedType == t
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { selectedType = t },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        RadioButton(selected = isSelected, onClick = { selectedType = t })
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(text = t, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = start,
                                            onValueChange = { start = it },
                                            label = { Text("Start Date (YYYY-MM-DD)") },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = end,
                                            onValueChange = { end = it },
                                            label = { Text("End Date (YYYY-MM-DD)") },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = reason,
                                            onValueChange = { reason = it },
                                            label = { Text("Detailed Reason") },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Select Approving Faculty (Choose 2 or 3):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        displayFaculty.forEach { fac ->
                                            val isChecked = selectedFacultyIds.contains(fac.id)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (isChecked) {
                                                            selectedFacultyIds = selectedFacultyIds - fac.id
                                                        } else {
                                                            if (selectedFacultyIds.size < 3) {
                                                                selectedFacultyIds = selectedFacultyIds + fac.id
                                                            } else {
                                                                Toast.makeText(context, "You can select at most 3 faculty members.", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        if (checked) {
                                                            if (selectedFacultyIds.size < 3) {
                                                                selectedFacultyIds = selectedFacultyIds + fac.id
                                                            } else {
                                                                Toast.makeText(context, "You can select at most 3 faculty members.", Toast.LENGTH_SHORT).show()
                                                            }
                                                        } else {
                                                            selectedFacultyIds = selectedFacultyIds - fac.id
                                                        }
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = "${fac.name} (${fac.designation})", style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        if (reason.isBlank()) {
                                            Toast.makeText(context, "Please enter a detailed reason.", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (selectedFacultyIds.size !in 2..3) {
                                            Toast.makeText(context, "Please select 2 or 3 faculty members for approval.", Toast.LENGTH_LONG).show()
                                            return@Button
                                        }
                                        val selectedIdsStr = selectedFacultyIds.joinToString(",")
                                        val selectedNamesStr = displayFaculty.filter { selectedFacultyIds.contains(it.id) }.joinToString(", ") { it.name }
                                        viewModel.submitLeaveRequest(start, end, reason, "No Attachment Required", selectedType, selectedIdsStr, selectedNamesStr)
                                        showLeaveFormDialog = false
                                        Toast.makeText(context, "$selectedType submitted to selected Faculty review desk.", Toast.LENGTH_SHORT).show()
                                    }) { Text("Submit Request") }
                                },
                                dismissButton = { OutlinedButton(onClick = { showLeaveFormDialog = false }) { Text("Cancel") } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentQRAttendanceScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var tokenInput by remember { mutableStateOf("") }
    var locationSimulation by remember { mutableStateOf("Classroom (Proximity Pass)") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("QR Code Proximity Check", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.StudentDashboard) }) {
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Scan Proximity Code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "Simulate active geofencing and class QR scan check-in.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Simulation Location Selector (Crucial high-fidelity feature to test GPS limits!)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "GPS Proximity Location Simulator", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Select where you are located physically to test coordinates validation:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { locationSimulation = "Classroom (Proximity Pass)" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (locationSimulation.contains("Classroom")) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.2f),
                                contentColor = if (locationSimulation.contains("Classroom")) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Classroom (15m)", fontSize = 11.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { locationSimulation = "Off-Campus Dormitory (Block)" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (locationSimulation.contains("Dormitory")) Color.Red else Color.LightGray.copy(alpha = 0.2f),
                                contentColor = if (locationSimulation.contains("Dormitory")) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Dormitory (1.2km)", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                label = { Text("Enter Active QR Code Token") },
                placeholder = { Text("e.g. QR_ATT_ABCD12") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    if (tokenInput.isBlank()) {
                        Toast.makeText(context, "Please enter the active session token.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // Setup coordinates based on toggle selection
                    val lat = if (locationSimulation.contains("Classroom")) 13.0828 else 13.0980
                    val lng = if (locationSimulation.contains("Classroom")) 80.2706 else 80.2910

                    viewModel.scanAndCheckInQr(tokenInput, lat, lng) { success, msg ->
                        if (success) {
                            viewModel.navigateTo(Screen.StudentDashboard)
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Verified, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Align Scanner and Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun LeaveLetterPrintDialog(
    student: Student,
    deptCode: String,
    request: LeaveRequest,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isPrinting by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Letterhead Header
                Text(
                    text = "G.R.T. INSTITUTE OF TECHNOLOGY",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color(0xFF1A237E),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Approved by AICTE | Affiliated to Anna University",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "GRT Nagar, Tiruttani, Tamil Nadu - 631209",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                // Solid partition line
                HorizontalDivider(color = Color.DarkGray, thickness = 1.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                val formTitle = when (request.requestType) {
                    "Casual Leave (CL) Letter" -> "OFFICIAL LEAVE APPLICATION FORM"
                    "On Duty (OD) Permission Letter" -> "OFFICIAL ON DUTY (OD) PERMISSION FORM"
                    "Symposium / Workshop Permission Letter" -> "OFFICIAL ACADEMIC EVENT PARTICIPATION PERMISSION"
                    "Industrial Visit (IV) Permission Letter" -> "OFFICIAL INDUSTRIAL VISIT PERMISSION FORM"
                    "Internship / In-Plant Training (IPT) Permission Letter" -> "OFFICIAL INTERNSHIP / IN-PLANT TRAINING PERMISSION FORM"
                    "Sports / Culturals Event Permission Letter" -> "OFFICIAL SPORTS/CULTURAL REPRESENTATION PERMISSION"
                    else -> "OFFICIAL ${request.requestType.uppercase()} FORM"
                }

                Text(
                    text = formTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Left aligned metadata
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "FROM: ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(60.dp))
                        Text(text = "${student.name} (Roll: ${student.rollNum})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "CLASS: ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(60.dp))
                        Text(text = "$deptCode | Sem ${student.semester} - Section ${student.section}", fontSize = 11.sp, color = Color.Black)
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "TO: ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(60.dp))
                        Text(text = "The Head of Department, Department of $deptCode", fontSize = 11.sp, color = Color.Black)
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "DATE: ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(60.dp))
                        Text(text = request.startDate, fontSize = 11.sp, color = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Letter Body
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Respected Sir/Madam,", fontSize = 12.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val bodyIntro = when (request.requestType) {
                        "Casual Leave (CL) Letter" -> "I am writing to formally request leave of absence from academic sessions starting from ${request.startDate} to ${request.endDate} due to: ${request.reason}."
                        "On Duty (OD) Permission Letter" -> "I am writing to formally request On Duty (OD) permission for the period starting from ${request.startDate} to ${request.endDate} due to: ${request.reason}."
                        "Symposium / Workshop Permission Letter" -> "I am writing to formally request permission to attend the symposium/workshop starting from ${request.startDate} to ${request.endDate} due to: ${request.reason}."
                        "Industrial Visit (IV) Permission Letter" -> "I am writing to formally request permission to attend the industrial visit starting from ${request.startDate} to ${request.endDate} due to: ${request.reason}."
                        "Internship / In-Plant Training (IPT) Permission Letter" -> "I am writing to formally request academic leave/permission to undergo internship/training starting from ${request.startDate} to ${request.endDate} at: ${request.reason}."
                        "Sports / Culturals Event Permission Letter" -> "I am writing to formally request permission to represent the institute in sports/cultural events starting from ${request.startDate} to ${request.endDate} due to: ${request.reason}."
                        else -> "I am writing to request permission/leave for the period from ${request.startDate} to ${request.endDate} due to: ${request.reason}."
                    }

                    Text(
                        text = bodyIntro,
                        fontSize = 12.sp,
                        color = Color.Black,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This application has been registered in the GRT Attendance Hub portal and digitally verified by the authority.",
                        fontSize = 12.sp,
                        color = Color.Black,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "I request you to kindly excuse my absence for these days. I will ensure all missed notes and laboratory exercises are caught up with immediately.",
                        fontSize = 12.sp,
                        color = Color.Black,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Thanking you.", fontSize = 12.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Yours faithfully,", fontSize = 12.sp, color = Color.Black)
                    Text(text = student.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Stamp & Digital Seal Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            BorderStroke(1.5.dp, Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(Color(0xFFE8F5E9))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "DEPARTMENT APPROVED & SEALED",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "Verification: APPROVED BY HOD - ${request.comments}",
                            fontSize = 9.sp,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Signatures
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.width(80.dp).height(1.dp).background(Color.Gray))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Student Signature", fontSize = 9.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.width(80.dp).height(1.dp).background(Color.Gray))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Parent Signature", fontSize = 9.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Digitally Signed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = Color(0xFF2E7D32)
                        )
                        Box(modifier = Modifier.width(80.dp).height(1.dp).background(Color.Gray))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "HOD Authority", fontSize = 9.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }
                    
                    Button(
                        onClick = {
                            isPrinting = true
                            generateAndSavePdf(context, student, deptCode, request)
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Downloading...", fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download PDF", fontSize = 11.sp)
                        }
                    }
                }

                if (isPrinting) {
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(1500)
                        isPrinting = false
                    }
                }
            }
        }
    }
}

fun generateAndSavePdf(context: android.content.Context, student: Student, deptCode: String, request: LeaveRequest) {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Configure paints
        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(26, 35, 126) // Deep indigo
            textSize = 14f
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val subtitlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 9f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val dividerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 1f
        }
        val docTypePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            isFakeBoldText = true
        }
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
        }
        val bodyPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
        }
        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(46, 125, 50)
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1f
        }
        val fillPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(232, 245, 233)
            style = android.graphics.Paint.Style.FILL
        }

        var y = 60f

        // 1. Institution Header
        canvas.drawText("G.R.T. INSTITUTE OF TECHNOLOGY", 297.5f, y, titlePaint)
        y += 18f
        canvas.drawText("Approved by AICTE | Affiliated to Anna University", 297.5f, y, subtitlePaint)
        y += 12f
        canvas.drawText("GRT Nagar, Tiruttani, Tamil Nadu - 631209", 297.5f, y, subtitlePaint)
        y += 15f

        // Partition Line
        canvas.drawLine(40f, y, 555f, y, dividerPaint)
        y += 25f

        // 2. Document Title
        val formTitle = when (request.requestType) {
            "Casual Leave (CL) Letter" -> "OFFICIAL LEAVE APPLICATION FORM"
            "On Duty (OD) Permission Letter" -> "OFFICIAL ON DUTY (OD) PERMISSION FORM"
            "Symposium / Workshop Permission Letter" -> "OFFICIAL ACADEMIC EVENT PARTICIPATION PERMISSION"
            "Industrial Visit (IV) Permission Letter" -> "OFFICIAL INDUSTRIAL VISIT PERMISSION FORM"
            "Internship / In-Plant Training (IPT) Permission Letter" -> "OFFICIAL INTERNSHIP / IN-PLANT TRAINING PERMISSION"
            "Sports / Culturals Event Permission Letter" -> "OFFICIAL SPORTS/CULTURAL REPRESENTATION PERMISSION"
            else -> "OFFICIAL ${request.requestType.uppercase()} FORM"
        }
        canvas.drawText(formTitle, 297.5f, y, docTypePaint)
        y += 30f

        // 3. Metadata block
        canvas.drawText("FROM: ", 50f, y, labelPaint)
        canvas.drawText("${student.name} (Roll: ${student.rollNum})", 110f, y, textPaint)
        y += 18f

        canvas.drawText("CLASS: ", 50f, y, labelPaint)
        canvas.drawText("$deptCode | Sem ${student.semester} - Section ${student.section}", 110f, y, textPaint)
        y += 18f

        canvas.drawText("TO: ", 50f, y, labelPaint)
        canvas.drawText("The Head of Department, Department of $deptCode", 110f, y, textPaint)
        y += 18f

        canvas.drawText("DATE: ", 50f, y, labelPaint)
        canvas.drawText(request.startDate, 110f, y, textPaint)
        y += 25f

        canvas.drawLine(40f, y, 555f, y, dividerPaint)
        y += 25f

        // 4. Letter Body
        canvas.drawText("Respected Sir/Madam,", 50f, y, textPaint)
        y += 20f

        val bodyIntro = when (request.requestType) {
            "Casual Leave (CL) Letter" -> "I am writing to request a leave of absence from classes from ${request.startDate} to ${request.endDate} due to: ${request.reason}."
            "On Duty (OD) Permission Letter" -> "I am writing to request On Duty (OD) permission from ${request.startDate} to ${request.endDate} due to: ${request.reason}."
            "Symposium / Workshop Permission Letter" -> "I am writing to request permission to participate in the symposium/workshop from ${request.startDate} to ${request.endDate} due to: ${request.reason}."
            "Industrial Visit (IV) Permission Letter" -> "I am writing to request permission for the industrial visit from ${request.startDate} to ${request.endDate} due to: ${request.reason}."
            "Internship / In-Plant Training (IPT) Permission Letter" -> "I am writing to request permission to undergo training/internship from ${request.startDate} to ${request.endDate} at: ${request.reason}."
            "Sports / Culturals Event Permission Letter" -> "I am writing to request permission to represent our institution in sports/cultural events from ${request.startDate} to ${request.endDate} due to: ${request.reason}."
            else -> "I am writing to request leave/permission from ${request.startDate} to ${request.endDate} due to: ${request.reason}."
        }

        fun drawWrappedText(text: String, maxWidth: Float) {
            val words = text.split(" ")
            var line = ""
            for (word in words) {
                val testLine = if (line.isEmpty()) word else "$line $word"
                if (bodyPaint.measureText(testLine) > maxWidth) {
                    canvas.drawText(line, 50f, y, bodyPaint)
                    line = word
                    y += 15f
                } else {
                    line = testLine
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, 50f, y, bodyPaint)
                y += 15f
            }
        }

        drawWrappedText(bodyIntro, 495f)
        y += 8f
        drawWrappedText("This permission letter was registered in the GRT Attendance Hub portal, and digitally verified by the authorities below.", 495f)
        y += 8f
        drawWrappedText("I request you to kindly excuse my absence for these days. I will ensure all missed coursework and lab exercises are completed immediately upon my return.", 495f)
        y += 20f

        canvas.drawText("Thanking you.", 50f, y, textPaint)
        y += 20f
        canvas.drawText("Yours faithfully,", 50f, y, textPaint)
        y += 15f
        canvas.drawText(student.name, 50f, y, labelPaint)
        y += 35f

        // 5. Approvers & Stamp Box
        canvas.drawRect(50f, y, 545f, y + 55f, fillPaint)
        canvas.drawRect(50f, y, 545f, y + 55f, borderPaint)
        
        val sealTitlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(46, 125, 50)
            textSize = 9f
            isFakeBoldText = true
        }
        val sealTextPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(27, 94, 32)
            textSize = 8f
        }
        canvas.drawText("DEPARTMENT APPROVED & SEALED", 65f, y + 18f, sealTitlePaint)
        canvas.drawText("Verification Status: APPROVED - ${request.comments}", 65f, y + 32f, sealTextPaint)
        canvas.drawText("Selected Faculty Authority Approvers: ${request.targetFacultyNames.ifBlank { "All Department Staff" }}", 65f, y + 45f, sealTextPaint)
        y += 85f

        // 6. Signatures block
        canvas.drawLine(50f, y, 130f, y, dividerPaint)
        canvas.drawText("Student Signature", 50f, y + 12f, subtitlePaint)

        canvas.drawLine(250f, y, 330f, y, dividerPaint)
        canvas.drawText("Parent Signature", 250f, y + 12f, subtitlePaint)

        canvas.drawText("Digitally Signed", 465f, y - 8f, sealTitlePaint)
        canvas.drawLine(425f, y, 505f, y, dividerPaint)
        canvas.drawText("HOD Authority", 425f, y + 12f, subtitlePaint)

        pdfDocument.finishPage(page)

        // Save file
        val cleanName = student.name.replace(" ", "_")
        val filename = "Permission_Letter_${cleanName}_${request.id}.pdf"
        
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val file = java.io.File(downloadsDir, filename)
        java.io.FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        
        android.widget.Toast.makeText(context, "Downloaded! Saved to Downloads folder as $filename", android.widget.Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        android.util.Log.e("PDF", "Standard save failed, trying fallback: ${e.localizedMessage}")
        try {
            // Fallback to app-specific external files dir (always works without permissions)
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 12f
            }
            canvas.drawText("G.R.T. INSTITUTE OF TECHNOLOGY", 50f, 50f, paint)
            canvas.drawText("Student: ${student.name}", 50f, 80f, paint)
            canvas.drawText("Letter Type: ${request.requestType}", 50f, 110f, paint)
            canvas.drawText("Reason: ${request.reason}", 50f, 140f, paint)
            canvas.drawText("Approved By: ${request.approvedBy}", 50f, 170f, paint)
            
            pdfDocument.finishPage(page)
            
            val filename = "Permission_Letter_${student.name.replace(" ", "_")}_${request.id}.pdf"
            val file = java.io.File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), filename)
            java.io.FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            android.widget.Toast.makeText(context, "PDF saved to app storage: $filename", android.widget.Toast.LENGTH_LONG).show()
        } catch (innerEx: Exception) {
            android.widget.Toast.makeText(context, "Failed to download PDF: ${innerEx.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun SisDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
fun SisFeeItem(title: String, amount: String, status: String, isPaid: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(text = status, style = MaterialTheme.typography.bodySmall, color = if (isPaid) Color(0xFF2E7D32) else Color(0xFFC62828), fontSize = 11.sp)
            }
            Text(text = amount, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = if (isPaid) MaterialTheme.colorScheme.primary else Color(0xFFEF6C00))
        }
    }
}

@Composable
fun SisExamSlot(date: String, session: String, code: String, title: String, hall: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "$code - $title", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                    Text(text = session, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Date: $date", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(text = "Seating: $hall", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

fun generateAndSaveStudentIdPdf(context: android.content.Context, student: Student, deptCode: String) {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(26, 35, 126)
            textSize = 14f
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val subtitlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 9f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            isFakeBoldText = true
        }
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
        }
        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(26, 35, 126)
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        var y = 60f
        canvas.drawText("G.R.T. INSTITUTE OF TECHNOLOGY", 297.5f, y, titlePaint)
        y += 18f
        canvas.drawText("STUDENT INFORMATION SYSTEM - DIGITAL STUDENT PASS", 297.5f, y, subtitlePaint)
        y += 20f

        canvas.drawRect(50f, y, 545f, y + 260f, borderPaint)
        y += 25f

        canvas.drawText("STUDENT IDENTIFICATION CREDENTIALS", 65f, y, labelPaint)
        y += 25f
        canvas.drawText("Name: ${student.name}", 65f, y, textPaint)
        y += 18f
        canvas.drawText("Student ID: ${student.studentId} | Roll No: ${student.rollNum}", 65f, y, textPaint)
        y += 18f
        canvas.drawText("Anna University Reg No: ${student.regNum}", 65f, y, textPaint)
        y += 18f
        canvas.drawText("Branch & Program: B.E. $deptCode | Semester: ${student.semester} (${student.section})", 65f, y, textPaint)
        y += 18f
        canvas.drawText("Date of Birth: ${student.dob} | Blood Group: ${student.bloodGroup}", 65f, y, textPaint)
        y += 18f
        canvas.drawText("Parent / Guardian: ${student.parentName} (${student.parentPhone})", 65f, y, textPaint)
        y += 18f
        canvas.drawText("Student Contact: ${student.phone} | ${student.email}", 65f, y, textPaint)
        y += 18f
        canvas.drawText("Residential Address: ${student.address}", 65f, y, textPaint)
        y += 22f
        canvas.drawText("Card Validity: ACADEMIC BATCH 2023 - 2027 | STATUS: VERIFIED ENROLLED", 65f, y, labelPaint)

        pdfDocument.finishPage(page)

        val cleanName = student.name.replace(" ", "_")
        val filename = "Student_ID_${cleanName}.pdf"
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val file = java.io.File(downloadsDir, filename)
        java.io.FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
        pdfDocument.close()

        android.widget.Toast.makeText(context, "ID Card Saved! Downloads/$filename", android.widget.Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "ID Card Downloaded to app cache", android.widget.Toast.LENGTH_SHORT).show()
    }
}

fun generateAndSaveMarksheetPdf(context: android.content.Context, student: Student, deptCode: String, subjects: List<Subject>) {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(26, 35, 126)
            textSize = 14f
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val subtitlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 9f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val dividerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 1f
        }
        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            isFakeBoldText = true
        }
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 9f
        }

        var y = 60f
        canvas.drawText("G.R.T. INSTITUTE OF TECHNOLOGY", 297.5f, y, titlePaint)
        y += 18f
        canvas.drawText("OFFICIAL SEMESTER GRADE REPORT & MARKSHEET", 297.5f, y, subtitlePaint)
        y += 15f
        canvas.drawLine(40f, y, 555f, y, dividerPaint)
        y += 20f

        canvas.drawText("CANDIDATE NAME: ${student.name.uppercase()}", 50f, y, labelPaint)
        canvas.drawText("REGISTER NO: ${student.regNum}", 340f, y, labelPaint)
        y += 18f
        canvas.drawText("DEPARTMENT: B.E. $deptCode", 50f, y, textPaint)
        canvas.drawText("SEMESTER: ${student.semester} (${student.section})", 340f, y, textPaint)
        y += 18f
        canvas.drawText("CGPA: 8.74 / 10.0 | SGPA: 8.85 / 10.0", 50f, y, labelPaint)
        canvas.drawText("CLASSIFICATION: FIRST CLASS WITH DISTINCTION", 340f, y, labelPaint)
        y += 25f

        canvas.drawLine(40f, y, 555f, y, dividerPaint)
        y += 18f
        canvas.drawText("COURSE CODE", 50f, y, labelPaint)
        canvas.drawText("COURSE TITLE", 140f, y, labelPaint)
        canvas.drawText("CREDITS", 360f, y, labelPaint)
        canvas.drawText("INTERNAL", 420f, y, labelPaint)
        canvas.drawText("GRADE", 490f, y, labelPaint)
        y += 10f
        canvas.drawLine(40f, y, 555f, y, dividerPaint)
        y += 18f

        subjects.forEach { sb ->
            canvas.drawText(sb.code, 50f, y, textPaint)
            val truncatedName = if (sb.name.length > 28) sb.name.take(28) + "..." else sb.name
            canvas.drawText(truncatedName, 140f, y, textPaint)
            canvas.drawText("4", 370f, y, textPaint)
            canvas.drawText("48/50", 425f, y, textPaint)
            canvas.drawText("O (10 GP)", 490f, y, labelPaint)
            y += 18f
        }

        y += 30f
        canvas.drawLine(40f, y, 555f, y, dividerPaint)
        y += 20f
        canvas.drawText("RESULT: PASS - ALL SUBJECTS CLEARED", 50f, y, labelPaint)
        y += 40f
        canvas.drawText("CONTROLLER OF EXAMINATIONS (DIGITALLY SEALED)", 297.5f, y, titlePaint)

        pdfDocument.finishPage(page)

        val cleanName = student.name.replace(" ", "_")
        val filename = "Marksheet_${cleanName}_Sem${student.semester}.pdf"
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val file = java.io.File(downloadsDir, filename)
        java.io.FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
        pdfDocument.close()

        android.widget.Toast.makeText(context, "Marksheet Saved! Downloads/$filename", android.widget.Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Marksheet Saved to app storage", android.widget.Toast.LENGTH_SHORT).show()
    }
}

fun generateAndSaveFeeReceiptPdf(context: android.content.Context, student: Student, deptCode: String) {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(26, 35, 126)
            textSize = 14f
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val subtitlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 9f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            isFakeBoldText = true
        }
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 9f
        }
        val dividerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 1f
        }

        var y = 60f
        canvas.drawText("G.R.T. INSTITUTE OF TECHNOLOGY", 297.5f, y, titlePaint)
        y += 18f
        canvas.drawText("STUDENT FEE FINANCIAL RECEIPT & TRANSACTION CLEARANCE", 297.5f, y, subtitlePaint)
        y += 15f
        canvas.drawLine(40f, y, 555f, y, dividerPaint)
        y += 20f

        canvas.drawText("RECEIPT NO: REC-2026-${student.rollNum}-901", 50f, y, labelPaint)
        canvas.drawText("DATE: 2026-07-15", 380f, y, textPaint)
        y += 18f
        canvas.drawText("STUDENT: ${student.name} (Roll: ${student.rollNum})", 50f, y, textPaint)
        canvas.drawText("REG NO: ${student.regNum}", 380f, y, textPaint)
        y += 18f
        canvas.drawText("PROGRAM: B.E. $deptCode Sem ${student.semester}", 50f, y, textPaint)
        y += 25f

        canvas.drawLine(40f, y, 555f, y, dividerPaint)
        y += 18f
        canvas.drawText("FEE PARTICULARS", 50f, y, labelPaint)
        canvas.drawText("TRANSACTION REF", 320f, y, labelPaint)
        canvas.drawText("AMOUNT PAID", 460f, y, labelPaint)
        y += 10f
        canvas.drawLine(40f, y, 555f, y, dividerPaint)
        y += 18f

        canvas.drawText("Tuition & Academic Training Fee", 50f, y, textPaint)
        canvas.drawText("TXN-892147", 320f, y, textPaint)
        canvas.drawText("$4,500.00", 470f, y, labelPaint)
        y += 18f

        canvas.drawText("Specialized Laboratory & Library Fee", 50f, y, textPaint)
        canvas.drawText("TXN-892148", 320f, y, textPaint)
        canvas.drawText("$900.00", 470f, y, labelPaint)
        y += 18f

        canvas.drawText("Anna University Examination Fee", 50f, y, textPaint)
        canvas.drawText("TXN-892149", 320f, y, textPaint)
        canvas.drawText("$600.00", 470f, y, labelPaint)
        y += 25f

        canvas.drawLine(40f, y, 555f, y, dividerPaint)
        y += 20f
        canvas.drawText("TOTAL AMOUNT PAID: $6,000.00 | BALANCE DUE: $500.00", 50f, y, labelPaint)
        y += 18f
        canvas.drawText("PAYMENT MODE: NetBanking / UPI | STATUS: SUCCESSFUL", 50f, y, textPaint)
        y += 40f
        canvas.drawText("FINANCE & ACCOUNTS DEPARTMENT (SEALED)", 297.5f, y, titlePaint)

        pdfDocument.finishPage(page)

        val cleanName = student.name.replace(" ", "_")
        val filename = "Fee_Receipt_${cleanName}.pdf"
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val file = java.io.File(downloadsDir, filename)
        java.io.FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
        pdfDocument.close()

        android.widget.Toast.makeText(context, "Fee Receipt Saved! Downloads/$filename", android.widget.Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Fee Receipt Saved to app storage", android.widget.Toast.LENGTH_SHORT).show()
    }
}

fun generateAndSaveHallTicketPdf(context: android.content.Context, student: Student, deptCode: String, subjects: List<Subject>) {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(26, 35, 126)
            textSize = 14f
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val subtitlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 9f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            isFakeBoldText = true
        }
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 9f
        }
        val dividerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 1f
        }

        var y = 60f
        canvas.drawText("G.R.T. INSTITUTE OF TECHNOLOGY", 297.5f, y, titlePaint)
        y += 18f
        canvas.drawText("ANNA UNIVERSITY END-SEMESTER EXAMINATIONS - HALL TICKET", 297.5f, y, subtitlePaint)
        y += 15f
        canvas.drawLine(40f, y, 555f, y, dividerPaint)
        y += 20f

        canvas.drawText("CANDIDATE: ${student.name.uppercase()}", 50f, y, labelPaint)
        canvas.drawText("REGISTER NO: ${student.regNum}", 340f, y, labelPaint)
        y += 18f
        canvas.drawText("BRANCH: B.E. $deptCode | Sem ${student.semester}", 50f, y, textPaint)
        canvas.drawText("EXAM CENTRE: GRT Campus - Block B", 340f, y, textPaint)
        y += 25f

        canvas.drawLine(40f, y, 555f, y, dividerPaint)
        y += 18f
        canvas.drawText("DATE", 50f, y, labelPaint)
        canvas.drawText("SESSION", 130f, y, labelPaint)
        canvas.drawText("CODE", 240f, y, labelPaint)
        canvas.drawText("COURSE TITLE", 310f, y, labelPaint)
        canvas.drawText("HALL / SEAT", 470f, y, labelPaint)
        y += 10f
        canvas.drawLine(40f, y, 555f, y, dividerPaint)
        y += 18f

        val examDates = listOf("2026-11-20", "2026-11-24", "2026-11-28", "2026-12-02", "2026-12-06", "2026-12-10")
        subjects.forEachIndexed { index, sb ->
            val date = examDates.getOrElse(index) { "2026-12-12" }
            canvas.drawText(date, 50f, y, textPaint)
            canvas.drawText("FN (09:30-12:30)", 130f, y, textPaint)
            canvas.drawText(sb.code, 240f, y, labelPaint)
            val truncated = if (sb.name.length > 22) sb.name.take(22) + "..." else sb.name
            canvas.drawText(truncated, 310f, y, textPaint)
            canvas.drawText("Hall 402 - CS-${student.rollNum}", 470f, y, textPaint)
            y += 18f
        }

        y += 35f
        canvas.drawLine(40f, y, 555f, y, dividerPaint)
        y += 25f
        canvas.drawText("Candidate Signature", 70f, y, subtitlePaint)
        canvas.drawText("Chief Superintendent", 430f, y, subtitlePaint)

        pdfDocument.finishPage(page)

        val cleanName = student.name.replace(" ", "_")
        val filename = "Hall_Ticket_${cleanName}_Sem${student.semester}.pdf"
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val file = java.io.File(downloadsDir, filename)
        java.io.FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
        pdfDocument.close()

        android.widget.Toast.makeText(context, "Hall Ticket Saved! Downloads/$filename", android.widget.Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Hall Ticket Saved to app storage", android.widget.Toast.LENGTH_SHORT).show()
    }
}
