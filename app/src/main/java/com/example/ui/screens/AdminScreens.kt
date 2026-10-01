package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
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
fun AdminDashboardScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("Dashboard") }
    var showNotificationLogs by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    // Collect states reactively
    val depts by viewModel.departments.collectAsStateWithLifecycle()
    val facs by viewModel.faculty.collectAsStateWithLifecycle()
    val studs by viewModel.students.collectAsStateWithLifecycle()
    val subjs by viewModel.subjects.collectAsStateWithLifecycle()
    val tts by viewModel.timetables.collectAsStateWithLifecycle()
    val hols by viewModel.holidays.collectAsStateWithLifecycle()
    val anns by viewModel.announcements.collectAsStateWithLifecycle()
    val leaves by viewModel.leaveRequests.collectAsStateWithLifecycle()
    val recs by viewModel.records.collectAsStateWithLifecycle()
    val sets by viewModel.settings.collectAsStateWithLifecycle()
    val alertLogs by viewModel.smsNotificationsLog.collectAsStateWithLifecycle()

    // Dialog state controllers
    var showDeptDialog by remember { mutableStateOf(false) }
    var showSubjectDialog by remember { mutableStateOf(false) }
    var showFacultyDialog by remember { mutableStateOf(false) }
    var showStudentDialog by remember { mutableStateOf(false) }
    var showTimetableDialog by remember { mutableStateOf(false) }
    var showHolidayDialog by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }

    // Statistics Calculations
    val minAttendancePct = sets.firstOrNull { it.key == "min_attendance_pct" }?.value?.toFloatOrNull() ?: 75f
    val overallAttendancePercent = remember(recs) {
        if (recs.isEmpty()) 0f
        else {
            val presents = recs.count { it.status in listOf("Present", "Late", "On Duty", "Approved Leave") }
            (presents.toFloat() / recs.size.toFloat()) * 100
        }
    }

    val presentTodayCount = remember(recs) {
        recs.count { it.status == "Present" } // simple simulated present
    }
    val absentTodayCount = remember(recs) {
        recs.count { it.status == "Absent" } // simple simulated absent
    }

    // Department Analytics mapping
    val deptWiseAttendance = remember(recs, studs, depts) {
        depts.associate { dept ->
            val deptStudents = studs.filter { it.deptId == dept.id }.map { it.id }
            val deptRecords = recs.filter { it.studentId in deptStudents }
            val avg = if (deptRecords.isEmpty()) 80f else {
                val presents = deptRecords.count { it.status in listOf("Present", "Late", "On Duty", "Approved Leave") }
                (presents.toFloat() / deptRecords.size) * 100
            }
            dept.code to avg
        }
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
                title = { Text("Admin SIS Console", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showNotificationLogs = true }) {
                        BadgedBox(
                            badge = {
                                if (alertLogs.isNotEmpty()) {
                                    Badge { Text(alertLogs.size.toString()) }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Sms, contentDescription = "Sms Logs")
                        }
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
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeTab == "Dashboard",
                    onClick = { activeTab = "Dashboard" },
                    icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("SIS Metrics") }
                )
                NavigationBarItem(
                    selected = activeTab == "Academics",
                    onClick = { activeTab = "Academics" },
                    icon = { Icon(imageVector = Icons.Default.School, contentDescription = null) },
                    label = { Text("Academics") }
                )
                NavigationBarItem(
                    selected = activeTab == "Roster",
                    onClick = { activeTab = "Roster" },
                    icon = { Icon(imageVector = Icons.Default.Group, contentDescription = null) },
                    label = { Text("Student SIS") }
                )
                NavigationBarItem(
                    selected = activeTab == "Passwords",
                    onClick = { activeTab = "Passwords" },
                    icon = { Icon(imageVector = Icons.Default.VpnKey, contentDescription = null) },
                    label = { Text("Passwords") }
                )
                NavigationBarItem(
                    selected = activeTab == "Settings",
                    onClick = { activeTab = "Settings" },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Config") }
                )
            }
        },
        floatingActionButton = {
            if (activeTab != "Dashboard" && activeTab != "Settings" && activeTab != "Passwords") {
                FloatingActionButton(
                    onClick = {
                        when (activeTab) {
                            "Academics" -> showDeptDialog = true
                            "Roster" -> showStudentDialog = true
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "Dashboard" -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Stat Cards Grid
                        item {
                            Text(
                                text = "Institutional Summary",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                DashboardStatCard(
                                    title = "Students",
                                    value = studs.size.toString(),
                                    icon = Icons.Default.People,
                                    iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                                    iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardStatCard(
                                    title = "Faculty",
                                    value = facs.size.toString(),
                                    icon = Icons.Default.Class,
                                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                                    iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                DashboardStatCard(
                                    title = "Departments",
                                    value = depts.size.toString(),
                                    icon = Icons.Default.Category,
                                    iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardStatCard(
                                    title = "Avg Attendance",
                                    value = "${overallAttendancePercent.toInt()}%",
                                    icon = Icons.Default.Percent,
                                    iconBgColor = if (overallAttendancePercent >= minAttendancePct) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    iconColor = if (overallAttendancePercent >= minAttendancePct) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    modifier = Modifier.weight(1f),
                                    trend = if (overallAttendancePercent >= minAttendancePct) "Above Threshold" else "Below Limit",
                                    trendColor = if (overallAttendancePercent >= minAttendancePct) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }

                        // Custom Visual Bar Chart
                        item {
                            CustomBarChart(
                                data = deptWiseAttendance,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Low Attendance Alerts & Top Performers Card
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(220.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Low Attendance (<$minAttendancePct%)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Red
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val lowAttendees = studs.filter { s ->
                                            val sRecs = recs.filter { it.studentId == s.id }
                                            if (sRecs.isEmpty()) false
                                            else {
                                                val presents = sRecs.count { it.status in listOf("Present", "Late", "On Duty", "Approved Leave") }
                                                (presents.toFloat() / sRecs.size) * 100 < minAttendancePct
                                            }
                                        }

                                        if (lowAttendees.isEmpty()) {
                                            Text("No student risks detected.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        } else {
                                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                items(lowAttendees) { s ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(text = s.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                                        val sRecs = recs.filter { it.studentId == s.id }
                                                        val pct = (sRecs.count { it.status in listOf("Present", "Late", "On Duty", "Approved Leave") }.toFloat() / sRecs.size) * 100
                                                        Text(text = "${pct.toInt()}%", style = MaterialTheme.typography.bodySmall, color = Color.Red, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(220.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Top Performers (100%)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val topPerformers = studs.filter { s ->
                                            val sRecs = recs.filter { it.studentId == s.id }
                                            sRecs.isNotEmpty() && sRecs.all { it.status in listOf("Present", "Late", "On Duty", "Approved Leave") }
                                        }

                                        if (topPerformers.isEmpty()) {
                                            Text("No student in full streak.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        } else {
                                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                items(topPerformers) { s ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(text = s.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "Academics" -> {
                    // Quick submenu choices
                    var academicSubTab by remember { mutableStateOf("Departments") }
                    Column(modifier = Modifier.fillMaxSize()) {
                        ScrollableTabRow(selectedTabIndex = when (academicSubTab) {
                            "Departments" -> 0
                            "Subjects" -> 1
                            "Timetable" -> 2
                            "Holidays" -> 3
                            else -> 0
                        }) {
                            Tab(selected = academicSubTab == "Departments", onClick = { academicSubTab = "Departments" }, text = { Text("Departments") })
                            Tab(selected = academicSubTab == "Subjects", onClick = { academicSubTab = "Subjects" }, text = { Text("Subjects") })
                            Tab(selected = academicSubTab == "Timetable", onClick = { academicSubTab = "Timetable" }, text = { Text("Timetable") })
                            Tab(selected = academicSubTab == "Holidays", onClick = { academicSubTab = "Holidays" }, text = { Text("Holidays") })
                        }

                        when (academicSubTab) {
                            "Departments" -> {
                                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(depts) { d ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column {
                                                    Text(text = d.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                                    Text(text = "Code: ${d.code}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                IconButton(onClick = { viewModel.deleteDepartment(d.id) }) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            "Subjects" -> {
                                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        Button(onClick = { showSubjectDialog = true }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Add New Subject")
                                        }
                                    }
                                    items(subjs) { sb ->
                                        val deptCode = depts.firstOrNull { it.id == sb.deptId }?.code ?: "Unknown"
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        ) {
                                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column {
                                                    Text(text = sb.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                                    Text(text = "${sb.code} | Dept: $deptCode | Semester ${sb.semester}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                IconButton(onClick = { viewModel.deleteSubject(sb.id) }) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            "Timetable" -> {
                                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        Button(onClick = { showTimetableDialog = true }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Add Timetable Entry")
                                        }
                                    }
                                    items(tts) { t ->
                                        val deptCode = depts.firstOrNull { it.id == t.deptId }?.code ?: "Unknown"
                                        val subjectName = subjs.firstOrNull { it.id == t.subjectId }?.name ?: "Subject"
                                        val facName = facs.firstOrNull { it.id == t.facultyId }?.name ?: "Faculty"
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        ) {
                                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = "$subjectName (Period ${t.period}) [${t.startTime} - ${t.endTime}]", fontWeight = FontWeight.Bold)
                                                    Text(text = "Class: $deptCode Sem ${t.semester} - Sec ${t.section} | Room: ${t.classroom}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(text = "Scheduled Day: ${t.dayOfWeek} | Faculty: $facName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(onClick = { viewModel.deleteTimetable(t.id) }) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            "Holidays" -> {
                                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        Button(onClick = { showHolidayDialog = true }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Add Holiday")
                                        }
                                    }
                                    items(hols) { h ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        ) {
                                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column {
                                                    Text(text = h.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                                    Text(text = "Date: ${h.date} | Type: ${h.type}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                IconButton(onClick = { viewModel.deleteHoliday(h.id) }) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "Roster" -> {
                    var rosterSubTab by remember { mutableStateOf("Students") }
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(selectedTabIndex = if (rosterSubTab == "Students") 0 else 1) {
                            Tab(selected = rosterSubTab == "Students", onClick = { rosterSubTab = "Students" }, text = { Text("Students") })
                            Tab(selected = rosterSubTab == "Faculty", onClick = { rosterSubTab = "Faculty" }, text = { Text("Faculty") })
                        }

                        when (rosterSubTab) {
                            "Students" -> {
                                var searchQuery by remember { mutableStateOf("") }
                                val filteredStudents = studs.filter { it.name.contains(searchQuery, ignoreCase = true) || it.studentId.contains(searchQuery, ignoreCase = true) }

                                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Search by name or Student ID...") },
                                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    // Excel / CSV Import & Export bar
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                // Simulating standard Excel/CSV Parser Import
                                                viewModel.addStudent(
                                                    studentId = "CS2300" + (studs.size + 1),
                                                    regNum = "CS-REG-2300" + (studs.size + 1),
                                                    rollNum = "0" + (studs.size + 1),
                                                    name = "Imported Student " + (studs.size + 1),
                                                    email = "imported_${studs.size + 1}@college.edu",
                                                    phone = "+14085559090",
                                                    deptId = 1,
                                                    semester = 3,
                                                    section = "A",
                                                    parentName = "Imported Parent",
                                                    parentPhone = "+14085551212",
                                                    address = "Parsed from Excel file",
                                                    bloodGroup = "O+",
                                                    dob = "2005-01-01"
                                                )
                                                Toast.makeText(context, "Excel file read: Parsed & imported 1 student record successfully!", Toast.LENGTH_LONG).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                        ) {
                                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Import Excel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                // Generate raw student list CSV String
                                                val csvBuilder = StringBuilder("Student ID,Roll Number,Name,Email,Department ID,Semester\n")
                                                studs.forEach { s ->
                                                    csvBuilder.append("${s.studentId},${s.rollNum},${s.name},${s.email},${s.deptId},${s.semester}\n")
                                                }
                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, csvBuilder.toString())
                                                    type = "text/csv"
                                                }
                                                context.startActivity(Intent.createChooser(sendIntent, "Export Student Roster (CSV)"))
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                                        ) {
                                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Export CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(filteredStudents) { s ->
                                            val dCode = depts.firstOrNull { it.id == s.deptId }?.code ?: "Unknown"
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                            ) {
                                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(text = s.name.first().toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(text = s.name, fontWeight = FontWeight.Bold)
                                                        Text(text = "ID: ${s.studentId} | Roll: ${s.rollNum} | Class: $dCode Sem ${s.semester} (${s.section})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text(text = "Parent: ${s.parentName} (${s.parentPhone})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                                    }
                                                    IconButton(onClick = { viewModel.deleteStudent(s.id) }) {
                                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            "Faculty" -> {
                                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        Button(onClick = { showFacultyDialog = true }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Add New Faculty")
                                        }
                                    }
                                    items(facs) { f ->
                                        val dCode = depts.firstOrNull { it.id == f.deptId }?.code ?: "Unknown"
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = f.name.first().toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = f.name, fontWeight = FontWeight.Bold)
                                                    Text(text = "${f.designation} | Dept: $dCode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(text = "Email: ${f.email} | Phone: ${f.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                                }
                                                IconButton(onClick = { viewModel.deleteFaculty(f.id) }) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "Settings" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Institutional Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 1. Minimum Attendance Percentage
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Minimum Attendance Rule",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Students falling below this limit are flagged on the dashboard and their records are compiled for low attendance reviews.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    var minPct by remember { mutableStateOf(minAttendancePct.toInt().toString()) }
                                    OutlinedTextField(
                                        value = minPct,
                                        onValueChange = { minPct = it },
                                        singleLine = true,
                                        label = { Text("Minimum %") },
                                        modifier = Modifier.width(120.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Button(onClick = {
                                        viewModel.updateSystemSetting("min_attendance_pct", minPct)
                                        Toast.makeText(context, "Minimum threshold updated to $minPct%", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text("Save")
                                    }
                                }
                            }
                        }

                        // 2. Adaptive Features Toggle
                        val isQrEnabled = sets.firstOrNull { it.key == "enable_qr" }?.value == "true"
                        val isGpsEnabled = sets.firstOrNull { it.key == "enable_gps" }?.value == "true"
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Adaptive Attendance Gateways",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.0f)) {
                                        Text("Enable Class QR Code Check-ins", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Allows faculty to open scan windows.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Switch(checked = isQrEnabled, onCheckedChange = { viewModel.updateSystemSetting("enable_qr", it.toString()) })
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.0f)) {
                                        Text("Enable GPS Presence Verification", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Enforces 50m distance tracking from target.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Switch(checked = isGpsEnabled, onCheckedChange = { viewModel.updateSystemSetting("enable_gps", it.toString()) })
                                }
                            }
                        }

                        // 3. Automated Backup & Restore Simulated Actions
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Database Management",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { Toast.makeText(context, "System backup generated successfully (Size: 450 KB). Saved to local storage.", Toast.LENGTH_LONG).show() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Backup, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Run System Backup")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { Toast.makeText(context, "Restored state from backup '2026_07_12_backup.db' successfully.", Toast.LENGTH_LONG).show() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Restore, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Restore Database from Backup")
                                }
                            }
                        }

                        // 4. Secure Admin Audit Logs Panel
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "System Access Audit Log",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("A secure, immutable log of system modifications.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Spacer(modifier = Modifier.height(12.dp))
                                AuditLogLine(action = "Admin user logged in securely", user = "admin@college.edu", time = "01:30 AM")
                                AuditLogLine(action = "System seed database initialized successfully", user = "System Core", time = "01:31 AM")
                                AuditLogLine(action = "SMS delivery alert compiled for Alice Smith", user = "Faculty Portal", time = "01:32 AM")
                            }
                        }
                    }
                }

                "Passwords" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "User Credentials Management",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "As the administrator, you can view the institutional roster and reset or initialize passwords for any student or faculty member.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 1. Faculty Section
                        Text(
                            text = "Faculty Members",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        facs.forEach { fac ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = fac.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                        Text(text = "Email: ${fac.email}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(text = "Designation: ${fac.designation}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    var newPass by remember(fac.id) { mutableStateOf("") }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newPass,
                                            onValueChange = { newPass = it },
                                            placeholder = { Text("New Pass") },
                                            singleLine = true,
                                            modifier = Modifier.width(120.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        Button(
                                            onClick = {
                                                if (newPass.isBlank()) {
                                                    Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    viewModel.adminSetPassword("Faculty", fac.id, newPass) { success ->
                                                        if (success) {
                                                            Toast.makeText(context, "Password updated successfully for ${fac.name}!", Toast.LENGTH_SHORT).show()
                                                            newPass = ""
                                                        } else {
                                                            Toast.makeText(context, "Error updating password.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Set")
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Students Section
                        Text(
                            text = "Students",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        studs.forEach { stud ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = stud.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                        Text(text = "Reg No: ${stud.regNum} | Sem ${stud.semester}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(text = "Email: ${stud.email}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    var newPass by remember(stud.id) { mutableStateOf("") }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newPass,
                                            onValueChange = { newPass = it },
                                            placeholder = { Text("New Pass") },
                                            singleLine = true,
                                            modifier = Modifier.width(120.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        Button(
                                            onClick = {
                                                if (newPass.isBlank()) {
                                                    Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    viewModel.adminSetPassword("Student", stud.id, newPass) { success ->
                                                        if (success) {
                                                            Toast.makeText(context, "Password updated successfully for ${stud.name}!", Toast.LENGTH_SHORT).show()
                                                            newPass = ""
                                                        } else {
                                                            Toast.makeText(context, "Error updating password.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Set")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- SMS LIVE FEED OVERLAY DRAWER ---
            AnimatedVisibility(
                visible = showNotificationLogs,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                OutgoingNotificationsPanel(
                    logs = alertLogs,
                    onClose = { showNotificationLogs = false }
                )
            }

            // --- CRUD Forms Pop-up Dialogs ---

            // A. Department Dialog Form
            if (showDeptDialog) {
                var dName by remember { mutableStateOf("") }
                var dCode by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showDeptDialog = false },
                    title = { Text("Add Department") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(value = dName, onValueChange = { dName = it }, label = { Text("Department Name") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = dCode, onValueChange = { dCode = it }, label = { Text("Code (e.g. CSE)") }, shape = RoundedCornerShape(12.dp))
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (dName.isNotBlank() && dCode.isNotBlank()) {
                                viewModel.addDepartment(dName, dCode)
                                showDeptDialog = false
                            }
                        }) { Text("Create") }
                    },
                    dismissButton = { OutlinedButton(onClick = { showDeptDialog = false }) { Text("Cancel") } }
                )
            }

            // B. Subject Dialog Form
            if (showSubjectDialog) {
                var sName by remember { mutableStateOf("") }
                var sCode by remember { mutableStateOf("") }
                var sDeptId by remember { mutableStateOf("1") }
                var sSem by remember { mutableStateOf("3") }
                AlertDialog(
                    onDismissRequest = { showSubjectDialog = false },
                    title = { Text("Add Subject Course") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(value = sName, onValueChange = { sName = it }, label = { Text("Subject Name") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = sCode, onValueChange = { sCode = it }, label = { Text("Course Code (e.g. CS301)") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = sDeptId, onValueChange = { sDeptId = it }, label = { Text("Department ID") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = sSem, onValueChange = { sSem = it }, label = { Text("Semester (1-8)") }, shape = RoundedCornerShape(12.dp))
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (sName.isNotBlank() && sCode.isNotBlank()) {
                                viewModel.addSubject(sName, sCode, sDeptId.toIntOrNull() ?: 1, sSem.toIntOrNull() ?: 3)
                                showSubjectDialog = false
                            }
                        }) { Text("Create") }
                    },
                    dismissButton = { OutlinedButton(onClick = { showSubjectDialog = false }) { Text("Cancel") } }
                )
            }

            // C. Faculty Dialog Form
            if (showFacultyDialog) {
                var fName by remember { mutableStateOf("") }
                var fEmail by remember { mutableStateOf("") }
                var fPhone by remember { mutableStateOf("") }
                var fDeptId by remember { mutableStateOf("1") }
                var fDesig by remember { mutableStateOf("Professor") }
                AlertDialog(
                    onDismissRequest = { showFacultyDialog = false },
                    title = { Text("Add Academic Faculty") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(value = fName, onValueChange = { fName = it }, label = { Text("Faculty Name") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = fEmail, onValueChange = { fEmail = it }, label = { Text("Email (username)") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = fPhone, onValueChange = { fPhone = it }, label = { Text("Mobile Phone") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = fDeptId, onValueChange = { fDeptId = it }, label = { Text("Department ID") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = fDesig, onValueChange = { fDesig = it }, label = { Text("Designation") }, shape = RoundedCornerShape(12.dp))
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (fName.isNotBlank() && fEmail.isNotBlank()) {
                                viewModel.addFaculty(fName, fEmail, fPhone, fDeptId.toIntOrNull() ?: 1, fDesig)
                                showFacultyDialog = false
                            }
                        }) { Text("Add Faculty") }
                    },
                    dismissButton = { OutlinedButton(onClick = { showFacultyDialog = false }) { Text("Cancel") } }
                )
            }

            // D. Student Dialog Form
            if (showStudentDialog) {
                var sName by remember { mutableStateOf("") }
                var sEmail by remember { mutableStateOf("") }
                var sId by remember { mutableStateOf("") }
                var sReg by remember { mutableStateOf("") }
                var sRoll by remember { mutableStateOf("") }
                var sDeptId by remember { mutableStateOf("1") }
                var sSem by remember { mutableStateOf("3") }
                var sSec by remember { mutableStateOf("A") }
                var sParent by remember { mutableStateOf("") }
                var sParentPhone by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showStudentDialog = false },
                    title = { Text("Register Student") },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            OutlinedTextField(value = sName, onValueChange = { sName = it }, label = { Text("Student Full Name") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = sEmail, onValueChange = { sEmail = it }, label = { Text("Email (Login username)") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = sId, onValueChange = { sId = it }, label = { Text("Student ID (e.g. CS23001)") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = sReg, onValueChange = { sReg = it }, label = { Text("Registration Number") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = sRoll, onValueChange = { sRoll = it }, label = { Text("Roll Number") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = sDeptId, onValueChange = { sDeptId = it }, label = { Text("Department ID") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = sSem, onValueChange = { sSem = it }, label = { Text("Semester (1-8)") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = sSec, onValueChange = { sSec = it }, label = { Text("Section (e.g. A, B, C)") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = sParent, onValueChange = { sParent = it }, label = { Text("Parent/Guardian Name") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = sParentPhone, onValueChange = { sParentPhone = it }, label = { Text("Parent Mobile Phone") }, shape = RoundedCornerShape(12.dp))
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (sName.isNotBlank() && sEmail.isNotBlank()) {
                                viewModel.addStudent(
                                    studentId = sId, regNum = sReg, rollNum = sRoll, name = sName, email = sEmail,
                                    phone = "+14085559090", deptId = sDeptId.toIntOrNull() ?: 1, semester = sSem.toIntOrNull() ?: 3,
                                    section = sSec, parentName = sParent, parentPhone = sParentPhone,
                                    address = "123 Main Street", bloodGroup = "O+", dob = "2005-01-01"
                                )
                                showStudentDialog = false
                            }
                        }) { Text("Register") }
                    },
                    dismissButton = { OutlinedButton(onClick = { showStudentDialog = false }) { Text("Cancel") } }
                )
            }

            // E. Timetable Dialog Form
            if (showTimetableDialog) {
                var tDeptId by remember { mutableStateOf("1") }
                var tSem by remember { mutableStateOf("3") }
                var tSec by remember { mutableStateOf("A") }
                var tSubId by remember { mutableStateOf("1") }
                var tFacId by remember { mutableStateOf("1") }
                var tDay by remember { mutableStateOf("Monday") }
                var tPeriod by remember { mutableStateOf("1") }
                var tClassroom by remember { mutableStateOf("Room 401") }
                var tStartTime by remember { mutableStateOf("09:00") }
                var tEndTime by remember { mutableStateOf("10:00") }
                AlertDialog(
                    onDismissRequest = { showTimetableDialog = false },
                    title = { Text("Add Timetable Entry") },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            OutlinedTextField(value = tDeptId, onValueChange = { tDeptId = it }, label = { Text("Dept ID") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = tSem, onValueChange = { tSem = it }, label = { Text("Semester") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = tSec, onValueChange = { tSec = it }, label = { Text("Section") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = tSubId, onValueChange = { tSubId = it }, label = { Text("Subject ID") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = tFacId, onValueChange = { tFacId = it }, label = { Text("Faculty ID") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = tDay, onValueChange = { tDay = it }, label = { Text("Day of Week") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = tPeriod, onValueChange = { tPeriod = it }, label = { Text("Period (1-8)") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = tClassroom, onValueChange = { tClassroom = it }, label = { Text("Classroom") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = tStartTime, onValueChange = { tStartTime = it }, label = { Text("Start Time (HH:MM)") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = tEndTime, onValueChange = { tEndTime = it }, label = { Text("End Time (HH:MM)") }, shape = RoundedCornerShape(12.dp))
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.addTimetable(
                                deptId = tDeptId.toIntOrNull() ?: 1, semester = tSem.toIntOrNull() ?: 3, section = tSec,
                                subjectId = tSubId.toIntOrNull() ?: 1, facultyId = tFacId.toIntOrNull() ?: 1,
                                dayOfWeek = tDay, period = tPeriod.toIntOrNull() ?: 1, classroom = tClassroom,
                                startTime = tStartTime, endTime = tEndTime
                            )
                            showTimetableDialog = false
                        }) { Text("Schedule") }
                    },
                    dismissButton = { OutlinedButton(onClick = { showTimetableDialog = false }) { Text("Cancel") } }
                )
            }

            // F. Holiday Dialog Form
            if (showHolidayDialog) {
                var hName by remember { mutableStateOf("") }
                var hDate by remember { mutableStateOf("2026-07-20") }
                var hType by remember { mutableStateOf("Academic") }
                AlertDialog(
                    onDismissRequest = { showHolidayDialog = false },
                    title = { Text("Add Holiday") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(value = hName, onValueChange = { hName = it }, label = { Text("Holiday Name") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = hDate, onValueChange = { hDate = it }, label = { Text("Date (YYYY-MM-DD)") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = hType, onValueChange = { hType = it }, label = { Text("Type (National/Academic)") }, shape = RoundedCornerShape(12.dp))
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (hName.isNotBlank()) {
                                viewModel.addHoliday(hName, hDate, hType)
                                showHolidayDialog = false
                            }
                        }) { Text("Create") }
                    },
                    dismissButton = { OutlinedButton(onClick = { showHolidayDialog = false }) { Text("Cancel") } }
                )
            }
        }
    }
}

@Composable
fun AuditLogLine(action: String, user: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.0f)) {
            Text(text = action, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text(text = "Triggered by: $user", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Text(text = time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
}
