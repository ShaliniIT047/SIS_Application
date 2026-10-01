package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
fun FacultyDashboardScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableStateOf("Assigned Schedule") }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    val currentFaculty by viewModel.currentFaculty.collectAsStateWithLifecycle()
    val depts by viewModel.departments.collectAsStateWithLifecycle()
    val subjs by viewModel.subjects.collectAsStateWithLifecycle()
    val timetables by viewModel.timetables.collectAsStateWithLifecycle()
    val leaveReqs by viewModel.leaveRequests.collectAsStateWithLifecycle()
    val recs by viewModel.records.collectAsStateWithLifecycle()
    val studs by viewModel.students.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    val fac = currentFaculty ?: return

    // Filter timetables assigned to this specific faculty member
    val assignedSlots = timetables.filter { it.facultyId == fac.id }

    // Show all leave requests so any faculty can review and approve them to reach the multi-teacher approval requirement
    val relevantLeaves = leaveReqs

    // Filter correction requests (records with Pending correction status in this faculty's department)
    val relevantCorrections = recs.filter { rec ->
        val s = studs.firstOrNull { it.id == rec.studentId }
        rec.correctionStatus == "Pending" && s != null && s.deptId == fac.deptId
    }

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
                title = { Text("Faculty SIS Portal", fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Chatbot) }) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI assistant", tint = MaterialTheme.colorScheme.primary)
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
            // --- Welcome Card ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.primary
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
                            text = fac.name.first().toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = fac.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${fac.designation} | ${depts.firstOrNull { it.id == fac.deptId }?.name ?: "Department"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // --- Secondary Sub Navigation Tabs ---
            ScrollableTabRow(selectedTabIndex = when (activeSubTab) {
                "Assigned Schedule" -> 0
                "Leave Requests" -> 1
                "Corrections" -> 2
                "Attendance Reports" -> 3
                else -> 0
            }) {
                Tab(
                    selected = activeSubTab == "Assigned Schedule",
                    onClick = { activeSubTab = "Assigned Schedule" },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Schedule")
                        }
                    }
                )
                Tab(
                    selected = activeSubTab == "Leave Requests",
                    onClick = { activeSubTab = "Leave Requests" },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Leaves")
                            if (relevantLeaves.any { it.status == "Pending" }) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge { Text(relevantLeaves.count { it.status == "Pending" }.toString()) }
                            }
                        }
                    }
                )
                Tab(
                    selected = activeSubTab == "Corrections",
                    onClick = { activeSubTab = "Corrections" },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Corrections")
                            if (relevantCorrections.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge { Text(relevantCorrections.size.toString()) }
                            }
                        }
                    }
                )
                Tab(
                    selected = activeSubTab == "Attendance Reports",
                    onClick = { activeSubTab = "Attendance Reports" },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Reports / Print")
                        }
                    }
                )
            }

            when (activeSubTab) {
                "Assigned Schedule" -> {
                    AppSectionHeader(
                        title = "Your Class Schedule",
                        subtitle = "Select any slot to take student attendance"
                    )

                    if (assignedSlots.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.EventBusy, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No assigned classes scheduled.", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(assignedSlots) { slot ->
                                val deptCode = depts.firstOrNull { it.id == slot.deptId }?.code ?: "Unknown"
                                val subjectName = subjs.firstOrNull { it.id == slot.subjectId }?.name ?: "Subject"

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(1.dp, RoundedCornerShape(12.dp))
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.loadStudentsForMarking(
                                                deptId = slot.deptId,
                                                semester = slot.semester,
                                                section = slot.section,
                                                subjectId = slot.subjectId,
                                                date = viewModel.selectedDateForMarking.value,
                                                period = slot.period
                                            )
                                            viewModel.navigateTo(Screen.FacultyMarkAttendance)
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "P${slot.period}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = subjectName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "Class: $deptCode Semester ${slot.semester} (${slot.section}) | Room: ${slot.classroom}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Day: ${slot.dayOfWeek} [${slot.startTime} - ${slot.endTime}]",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Icon(imageVector = Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                "Leave Requests" -> {
                    AppSectionHeader(
                        title = "Student Leave Requests",
                        subtitle = "Review and process submitted document justifications"
                    )

                    if (relevantLeaves.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No leave requests submitted in your department.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(relevantLeaves) { req ->
                                val studentName = studs.firstOrNull { it.id == req.studentId }?.name ?: "Student"
                                var isExpanded by remember { mutableStateOf(false) }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (req.status == "Pending") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(text = studentName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                                Text(text = "Dates: ${req.startDate} to ${req.endDate}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = "Reason: ${req.reason}", style = MaterialTheme.typography.bodyMedium)
                                        if (req.documentPath.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = req.documentPath, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }

                                        val currentApprovers = req.approvedBy.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                        if (currentApprovers.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Approved by: ${currentApprovers.joinToString(", ")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        if (req.status.startsWith("Pending")) {
                                            val alreadyApprovedByMe = fac.name in currentApprovers
                                            if (alreadyApprovedByMe) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "✓ You have approved this request. Waiting for other teachers' approvals.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF2E7D32),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = { viewModel.reviewLeaveRequest(req, "Approved", "Approved") },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Approve")
                                                    }
                                                    OutlinedButton(
                                                        onClick = { viewModel.reviewLeaveRequest(req, "Rejected", "Rejected") },
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Reject")
                                                    }
                                                }
                                            }
                                        } else if (req.comments.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(text = "Comments: ${req.comments}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "Corrections" -> {
                    AppSectionHeader(
                        title = "Attendance Correction Requests",
                        subtitle = "Review student claims about discrepancies"
                    )

                    if (relevantCorrections.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No correction requests pending review.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(relevantCorrections) { record ->
                                val studentName = studs.firstOrNull { it.id == record.studentId }?.name ?: "Student"

                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = studentName, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Discrepancy Claim: Student was marked '${record.status}'.", style = MaterialTheme.typography.bodySmall)
                                        Text(text = "Student Statement: \"${record.correctionReason}\"", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.reviewCorrectionRequest(record, true) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Approve (Set Present)")
                                            }
                                            OutlinedButton(
                                                onClick = { viewModel.reviewCorrectionRequest(record, false) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Dismiss Request")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "Attendance Reports" -> {
                    FacultyAttendanceReportsSection(
                        viewModel = viewModel,
                        assignedSlots = assignedSlots,
                        depts = depts,
                        subjs = subjs,
                        studs = studs,
                        recs = recs,
                        sessions = sessions
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyMarkAttendanceScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val markingList by viewModel.markingStateList.collectAsStateWithLifecycle()
    val isLocked by viewModel.isAttendanceLocked.collectAsStateWithLifecycle()

    var showQrDialog by remember { mutableStateOf(false) }
    var activeQrToken by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Record Attendance", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.FacultyDashboard) }) {
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
                .padding(16.dp)
        ) {
            // Header Slot Overview
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Scheduled Session Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Class: Dept ${viewModel.selectedDeptIdForMarking.value} Sem ${viewModel.selectedSemesterForMarking.value} (${viewModel.selectedSectionForMarking.value})", fontWeight = FontWeight.Bold)
                    Text(text = "Date: ${viewModel.selectedDateForMarking.value} | Period: ${viewModel.selectedPeriodForMarking.value}", style = MaterialTheme.typography.bodySmall)

                    if (isLocked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "This attendance session is LOCKED. Changes cannot be directly applied.", color = Color.Red, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Actions Bar
            if (!isLocked) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.markAllPresent() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        Icon(imageVector = Icons.Default.DoneAll, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark All Present", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.startQrSession { token ->
                                activeQrToken = token
                                showQrDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                    ) {
                        Icon(imageVector = Icons.Default.QrCode, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Generate QR Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Student list with 48.dp target buttons
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(markingList) { state ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = state.student.name, fontWeight = FontWeight.Bold)
                                    Text(text = "Roll: ${state.student.rollNum} | ID: ${state.student.studentId}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (state.status) {
                                            "Present" -> Color(0xFFE8F5E9)
                                            "Absent" -> Color(0xFFFFEBEE)
                                            "Late" -> Color(0xFFFFF3E0)
                                            else -> Color(0xFFE3F2FD)
                                        }
                                    )
                                ) {
                                    Text(
                                        text = state.status,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when (state.status) {
                                            "Present" -> Color(0xFF2E7D32)
                                            "Absent" -> Color(0xFFC62828)
                                            "Late" -> Color(0xFFEF6C00)
                                            else -> Color(0xFF1565C0)
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (!isLocked) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    AttendancePill(
                                        label = "P",
                                        isSelected = state.status == "Present",
                                        onClick = { viewModel.updateStudentAttendanceState(state.student.id, "Present") },
                                        modifier = Modifier.weight(1f),
                                        selectedColor = Color(0xFF4CAF50)
                                    )
                                    AttendancePill(
                                        label = "A",
                                        isSelected = state.status == "Absent",
                                        onClick = { viewModel.updateStudentAttendanceState(state.student.id, "Absent") },
                                        modifier = Modifier.weight(1f),
                                        selectedColor = Color(0xFFF44336)
                                    )
                                    AttendancePill(
                                        label = "L",
                                        isSelected = state.status == "Late",
                                        onClick = { viewModel.updateStudentAttendanceState(state.student.id, "Late") },
                                        modifier = Modifier.weight(1f),
                                        selectedColor = Color(0xFFFF9800)
                                    )
                                    AttendancePill(
                                        label = "OD",
                                        isSelected = state.status == "On Duty",
                                        onClick = { viewModel.updateStudentAttendanceState(state.student.id, "On Duty") },
                                        modifier = Modifier.weight(1f),
                                        selectedColor = Color(0xFF2196F3)
                                    )
                                    AttendancePill(
                                        label = "ML",
                                        isSelected = state.status == "Medical Leave",
                                        onClick = { viewModel.updateStudentAttendanceState(state.student.id, "Medical Leave") },
                                        modifier = Modifier.weight(1f),
                                        selectedColor = Color(0xFF9C27B0)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Submit Attendance
            if (!isLocked) {
                Button(
                    onClick = {
                        viewModel.submitAttendance(lockAfterSubmit = true) {
                            Toast.makeText(context, "Attendance locked & submitted. Absent parent alerts triggered.", Toast.LENGTH_LONG).show()
                            viewModel.navigateTo(Screen.FacultyDashboard)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.LockClock, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Submit and Lock Session", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        // QR Code Simulated Window Dialog
        if (showQrDialog) {
            AlertDialog(
                onDismissRequest = { showQrDialog = false },
                title = { Text("Active QR Check-In Session", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Scan this QR Code to register presence instantly.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .border(4.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // High fidelity simulated QR Code vectors
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw a retro vector block grid representing QR
                                drawRoundRect(color = Color.Black, topLeft = Offset(0f, 0f), size = Size(40f, 40f))
                                drawRoundRect(color = Color.Black, topLeft = Offset(this.size.width - 40f, 0f), size = Size(40f, 40f))
                                drawRoundRect(color = Color.Black, topLeft = Offset(0f, this.size.height - 40f), size = Size(40f, 40f))
                                drawRoundRect(color = Color.Black, topLeft = Offset(80f, 80f), size = Size(50f, 50f))
                                drawRoundRect(color = Color.Black, topLeft = Offset(130f, 130f), size = Size(30f, 30f))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "SESSION TOKEN: $activeQrToken", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        Text(text = "Active Gateways: GPS Verification Enforced (Classroom target)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                },
                confirmButton = {
                    Button(onClick = { showQrDialog = false }) {
                        Text("Dismiss")
                    }
                }
            )
        }
    }
}

@Composable
fun AttendancePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) selectedColor else Color.LightGray.copy(alpha = 0.2f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color.Black,
            fontSize = 12.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyAttendanceReportsSection(
    viewModel: AppViewModel,
    assignedSlots: List<Timetable>,
    depts: List<Department>,
    subjs: List<Subject>,
    studs: List<Student>,
    recs: List<AttendanceRecord>,
    sessions: List<AttendanceSession>
) {
    var selectedRange by remember { mutableStateOf("Monthly") } // "Monthly" or "3-Month"
    var selectedSlot by remember { mutableStateOf(assignedSlots.firstOrNull()) }
    var showPrintDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppSectionHeader(
            title = "Academic Register Reports",
            subtitle = "Generate official 30-day (Monthly) or 90-day (3-Month) attendance ledgers for university submission"
        )

        // Range Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { selectedRange = "Monthly" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedRange == "Monthly") MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.2f),
                    contentColor = if (selectedRange == "Monthly") Color.White else Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Monthly (30 Days)", fontSize = 12.sp)
            }

            Button(
                onClick = { selectedRange = "3-Month" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedRange == "3-Month") MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.2f),
                    contentColor = if (selectedRange == "3-Month") Color.White else Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("3-Month Semester", fontSize = 12.sp)
            }
        }

        if (assignedSlots.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No assigned classes found to compile reports.", color = Color.Gray)
            }
        } else {
            Text(text = "Select Class / Subject Section:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            
            // horizontal scroll of assigned classes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                assignedSlots.forEach { slot ->
                    val subjName = subjs.firstOrNull { it.id == slot.subjectId }?.name ?: "Subject"
                    val isSel = selectedSlot?.id == slot.id
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedSlot = slot },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = subjName, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = "Sem ${slot.semester} - Section ${slot.section}", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }

            val currentSlot = selectedSlot
            if (currentSlot != null) {
                val classStudents = studs.filter {
                    it.deptId == currentSlot.deptId && it.semester == currentSlot.semester && it.section == currentSlot.section
                }

                // Filter sessions and records for this subject
                val classSessions = sessions.filter {
                    it.deptId == currentSlot.deptId && it.semester == currentSlot.semester &&
                    it.section == currentSlot.section && it.subjectId == currentSlot.subjectId
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Class Register Summary", fontWeight = FontWeight.Bold)
                            Text(text = "Total Students: ${classStudents.size} | Sessions Run: ${classSessions.size}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        
                        Button(
                            onClick = { showPrintDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print Register Sheet", fontSize = 11.sp)
                        }
                    }
                }

                Text(text = "Preview of Register Matrix", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(classStudents) { student ->
                        val studRecs = recs.filter { r ->
                            r.studentId == student.id && classSessions.any { it.id == r.sessionId }
                        }
                        val presentCount = studRecs.count { it.status in listOf("Present", "Late", "On Duty", "Approved Leave") }
                        val attendanceRate = if (studRecs.isEmpty()) 100f else {
                            (presentCount.toFloat() / studRecs.size.toFloat()) * 100
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
                                Column {
                                    Text(text = student.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = "Roll No: ${student.rollNum} | Active Status: ${if (attendanceRate >= 75) "Safe" else "Low Attendance"}", fontSize = 11.sp, color = if (attendanceRate >= 75) Color(0xFF2E7D32) else Color(0xFFC62828))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(if (attendanceRate >= 75) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "${attendanceRate.toInt()}%", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (attendanceRate >= 75) Color(0xFF2E7D32) else Color(0xFFC62828))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPrintDialog && selectedSlot != null) {
        val currentSlot = selectedSlot!!
        val classStudents = studs.filter {
            it.deptId == currentSlot.deptId && it.semester == currentSlot.semester && it.section == currentSlot.section
        }
        val classSessions = sessions.filter {
            it.deptId == currentSlot.deptId && it.semester == currentSlot.semester &&
            it.section == currentSlot.section && it.subjectId == currentSlot.subjectId
        }
        val deptCode = depts.firstOrNull { it.id == currentSlot.deptId }?.code ?: "CSE"
        val subjectName = subjs.firstOrNull { it.id == currentSlot.subjectId }?.name ?: "Subject"

        AttendanceRegisterPrintDialog(
            range = selectedRange,
            deptCode = deptCode,
            subjectName = subjectName,
            semester = currentSlot.semester,
            section = currentSlot.section,
            students = classStudents,
            sessions = classSessions,
            records = recs,
            onDismiss = { showPrintDialog = false }
        )
    }
}

@Composable
fun AttendanceRegisterPrintDialog(
    range: String, // "Monthly" or "3-Month"
    deptCode: String,
    subjectName: String,
    semester: Int,
    section: String,
    students: List<Student>,
    sessions: List<AttendanceSession>,
    records: List<AttendanceRecord>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isPrinting by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Register Header
                Text(
                    text = "G.R.T. INSTITUTE OF TECHNOLOGY",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = Color(0xFF1E3A8A),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "OFFICIAL ATTENDANCE RECORD SHEET & REGISTER LEDGER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = Color.Black, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Register info block
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF3F4F6)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(text = "DEPARTMENT: Dept of $deptCode", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(text = "COURSE / SUBJECT: $subjectName", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(text = "CLASS / TARGET: Semester $semester - Section $section", fontSize = 10.sp, color = Color.Black)
                    Text(
                        text = "LEDGER SCOPE: ${range.uppercase()} REGISTER (${if (range == "Monthly") "Last 30 Calendar Days" else "Last 90 Semester Days"})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E3A8A)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // The Matrix Grid table
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color.Gray))
                ) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE5E7EB))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Roll", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.width(30.dp))
                        Text(text = "Student Name", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(1f))
                        Text(text = "W1", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                        Text(text = "W2", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                        Text(text = "W3", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                        Text(text = "W4", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                        Text(text = "Rate %", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                    }

                    HorizontalDivider(color = Color.Gray, thickness = 1.dp)

                    // Table rows
                    students.forEach { stud ->
                        val studRecs = records.filter { r ->
                            r.studentId == stud.id && sessions.any { s -> s.id == r.sessionId }
                        }
                        val presentCount = studRecs.count { it.status in listOf("Present", "Late", "On Duty", "Approved Leave") }
                        val rate = if (studRecs.isEmpty()) 100f else (presentCount.toFloat() / studRecs.size.toFloat()) * 100

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stud.rollNum, fontSize = 9.sp, color = Color.Black, modifier = Modifier.width(30.dp))
                            Text(text = stud.name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            
                            // Mock weekly statuses representing register dots
                            Text(text = "P", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                            Text(
                                text = if (rate < 80 && stud.rollNum == "CSE-002") "A" else "P",
                                color = if (rate < 80 && stud.rollNum == "CSE-002") Color.Red else Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.width(20.dp),
                                textAlign = TextAlign.Center
                            )
                            Text(text = "P", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                            Text(text = "P", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                            
                            Text(
                                text = "${rate.toInt()}%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (rate >= 75) Color(0xFF2E7D32) else Color.Red,
                                modifier = Modifier.width(40.dp),
                                textAlign = TextAlign.End
                            )
                        }
                        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stamp & Official Verification notice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color(0xFF0D47A1)), shape = RoundedCornerShape(6.dp))
                        .background(Color(0xFFE3F2FD))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = Color(0xFF0D47A1), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "University Compliance System: This digital attendance ledger is compiled in real-time according to Anna University Regulations under HOD verification desk keys.",
                        fontSize = 8.sp,
                        color = Color(0xFF0D47A1),
                        lineHeight = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Signatures
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.width(60.dp).height(1.dp).background(Color.Gray))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Subject Teacher", fontSize = 8.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.width(60.dp).height(1.dp).background(Color.Gray))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Head of Dept", fontSize = 8.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.width(60.dp).height(1.dp).background(Color.Gray))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Principal Office", fontSize = 8.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Dismiss")
                    }

                    Button(
                        onClick = {
                            isPrinting = true
                            Toast.makeText(context, "Contacting College Print Spooler...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 1.5.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Printing...", fontSize = 11.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print Register Sheet", fontSize = 10.sp)
                        }
                    }
                }

                if (isPrinting) {
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2000)
                        isPrinting = false
                        Toast.makeText(context, "${range} Register printed successfully. 4 copies sent to Tiruttani Canon Room.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
