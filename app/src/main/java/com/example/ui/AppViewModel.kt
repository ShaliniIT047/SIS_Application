package com.example.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class StudentAttendanceState(
    val student: Student,
    var status: String // "Present", "Absent", "Late", "On Duty", "Medical Leave", "Approved Leave"
)

data class SmsNotificationLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val recipientName: String,
    val toPhone: String,
    val message: String,
    val channel: String // "SMS (Twilio)" or "Email (Nodemailer Fallback)"
)

data class ChatbotMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val sender: String, // "User" or "AI"
    val timestamp: Long = System.currentTimeMillis()
)

class AppViewModel(application: Application, val repository: AppRepository) : AndroidViewModel(application) {

    // --- Navigation & Auth State ---
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _currentStudent = MutableStateFlow<Student?>(null)
    val currentStudent: StateFlow<Student?> = _currentStudent

    private val _currentFaculty = MutableStateFlow<Faculty?>(null)
    val currentFaculty: StateFlow<Faculty?> = _currentFaculty

    val prefilledUsername = MutableStateFlow("")
    val prefilledPassword = MutableStateFlow("")

    // --- Seeding & Database States ---
    val departments = repository.allDepartments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val faculty = repository.allFaculty.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val students = repository.allStudents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val subjects = repository.allSubjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val timetables = repository.allTimetables.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val holidays = repository.allHolidays.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val announcements = repository.allAnnouncements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val leaveRequests = repository.allLeaveRequests.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sessions = repository.allSessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val records = repository.allRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = repository.allSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Attendance Marking Form State ---
    private val _markingStateList = MutableStateFlow<List<StudentAttendanceState>>(emptyList())
    val markingStateList: StateFlow<List<StudentAttendanceState>> = _markingStateList

    var selectedDeptIdForMarking = MutableStateFlow(1)
    var selectedSemesterForMarking = MutableStateFlow(3)
    var selectedSectionForMarking = MutableStateFlow("A")
    var selectedSubjectIdForMarking = MutableStateFlow(1)
    var selectedClassroomForMarking = MutableStateFlow("Room 401")
    var selectedPeriodForMarking = MutableStateFlow(1)
    var selectedDateForMarking = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    var isAttendanceLocked = MutableStateFlow(false)

    // --- Notification logs ---
    private val _smsNotificationsLog = MutableStateFlow<List<SmsNotificationLogEntry>>(emptyList())
    val smsNotificationsLog: StateFlow<List<SmsNotificationLogEntry>> = _smsNotificationsLog

    // --- Chatbot State ---
    private val _chatbotMessages = MutableStateFlow<List<ChatbotMessage>>(listOf(
        ChatbotMessage(
            text = "Vanakkam & Welcome! I am your SIS AI Academic Advisor (மாணவர் தகவல் அமைப்பு ஆலோசகர்). Ask me anything about your academic biodata, semester marks, CGPA, attendance, fee dues, exam timetable, or multi-teacher leave letters in English or தமிழ்!",
            sender = "AI"
        )
    ))
    val chatbotMessages: StateFlow<List<ChatbotMessage>> = _chatbotMessages

    private val _isChatbotLoading = MutableStateFlow(false)
    val isChatbotLoading: StateFlow<Boolean> = _isChatbotLoading

    // --- AI Prediction State ---
    private val _predictionResult = MutableStateFlow<String?>(null)
    val predictionResult: StateFlow<String?> = _predictionResult

    private val _isPredictionLoading = MutableStateFlow(false)
    val isPredictionLoading: StateFlow<Boolean> = _isPredictionLoading

    // --- Initialize and Seed ---
    init {
        viewModelScope.launch {
            seedDatabaseIfRequired()
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // --- Auth Logic ---
    fun login(username: String, passwordHash: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByUsername(username)
            if (user != null && user.passwordHash == passwordHash) {
                _currentUser.value = user
                when (user.role) {
                    "Student" -> {
                        val student = repository.getStudentById(user.associatedId)
                        _currentStudent.value = student
                        _currentFaculty.value = null
                        _currentScreen.value = Screen.StudentDashboard
                    }
                    "Faculty" -> {
                        val facultyMember = repository.getFacultyById(user.associatedId)
                        _currentFaculty.value = facultyMember
                        _currentStudent.value = null
                        _currentScreen.value = Screen.FacultyDashboard
                    }
                    "Admin" -> {
                        _currentFaculty.value = null
                        _currentStudent.value = null
                        _currentScreen.value = Screen.AdminDashboard
                    }
                }
                onResult(true, "Login Successful")
            } else {
                onResult(false, "Invalid username or password")
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _currentStudent.value = null
        _currentFaculty.value = null
        _currentScreen.value = Screen.Home
    }

    fun adminSetPassword(role: String, associatedId: Int, newPassword: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val user = repository.getUserByRoleAndId(role, associatedId)
            if (user != null) {
                repository.updateUser(user.copy(passwordHash = newPassword))
                onResult(true)
            } else {
                val email = if (role == "Student") {
                    repository.getStudentById(associatedId)?.email
                } else {
                    repository.getFacultyById(associatedId)?.email
                }
                if (email != null) {
                    val u = repository.getUserByUsername(email)
                    if (u != null) {
                        repository.updateUser(u.copy(passwordHash = newPassword))
                        onResult(true)
                    } else {
                        repository.insertUser(User(username = email, passwordHash = newPassword, role = role, associatedId = associatedId))
                        onResult(true)
                    }
                } else {
                    onResult(false)
                }
            }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String, onResult: (Boolean, String) -> Unit) {
        val user = _currentUser.value
        if (user == null) {
            onResult(false, "No logged-in user")
            return
        }
        if (user.passwordHash != oldPassword) {
            onResult(false, "Incorrect old password")
            return
        }
        viewModelScope.launch {
            val updated = user.copy(passwordHash = newPassword)
            repository.updateUser(updated)
            _currentUser.value = updated
            onResult(true, "Password changed successfully")
        }
    }

    // --- CRUD: Department ---
    fun addDepartment(name: String, code: String) {
        viewModelScope.launch {
            repository.insertDepartment(Department(name = name, code = code))
        }
    }

    fun deleteDepartment(id: Int) {
        viewModelScope.launch {
            repository.deleteDepartment(id)
        }
    }

    // --- CRUD: Subjects ---
    fun addSubject(name: String, code: String, deptId: Int, semester: Int) {
        viewModelScope.launch {
            repository.insertSubject(Subject(name = name, code = code, deptId = deptId, semester = semester))
        }
    }

    fun deleteSubject(id: Int) {
        viewModelScope.launch {
            repository.deleteSubject(id)
        }
    }

    // --- CRUD: Faculty ---
    fun addFaculty(name: String, email: String, phone: String, deptId: Int, designation: String) {
        viewModelScope.launch {
            val fid = repository.insertFaculty(Faculty(name = name, email = email, phone = phone, deptId = deptId, designation = designation))
            repository.insertUser(User(username = email, passwordHash = "password123", role = "Faculty", associatedId = fid.toInt()))
        }
    }

    fun deleteFaculty(id: Int) {
        viewModelScope.launch {
            repository.deleteFaculty(id)
        }
    }

    // --- CRUD: Students ---
    fun addStudent(
        studentId: String, regNum: String, rollNum: String, name: String, email: String, phone: String,
        deptId: Int, semester: Int, section: String, parentName: String, parentPhone: String,
        address: String, bloodGroup: String, dob: String
    ) {
        viewModelScope.launch {
            val sid = repository.insertStudent(Student(
                studentId = studentId, regNum = regNum, rollNum = rollNum, name = name, email = email, phone = phone,
                deptId = deptId, semester = semester, section = section, parentName = parentName, parentPhone = parentPhone,
                address = address, bloodGroup = bloodGroup, dob = dob
            ))
            repository.insertUser(User(username = email, passwordHash = "password123", role = "Student", associatedId = sid.toInt()))
        }
    }

    fun deleteStudent(id: Int) {
        viewModelScope.launch {
            repository.deleteStudent(id)
        }
    }

    // --- CRUD: Timetables ---
    fun addTimetable(deptId: Int, semester: Int, section: String, subjectId: Int, facultyId: Int, dayOfWeek: String, period: Int, classroom: String, startTime: String = "09:00", endTime: String = "10:00") {
        viewModelScope.launch {
            repository.insertTimetable(Timetable(
                deptId = deptId, semester = semester, section = section, subjectId = subjectId,
                facultyId = facultyId, dayOfWeek = dayOfWeek, period = period, classroom = classroom,
                startTime = startTime, endTime = endTime
            ))
        }
    }

    fun deleteTimetable(id: Int) {
        viewModelScope.launch {
            repository.deleteTimetable(id)
        }
    }

    // --- CRUD: Holidays ---
    fun addHoliday(name: String, date: String, type: String) {
        viewModelScope.launch {
            repository.insertHoliday(Holiday(name = name, date = date, type = type))
        }
    }

    fun deleteHoliday(id: Int) {
        viewModelScope.launch {
            repository.deleteHoliday(id)
        }
    }

    // --- CRUD: Announcements ---
    fun addAnnouncement(title: String, content: String, targetDeptId: Int, targetSemester: Int, author: String, attachmentName: String = "") {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-mm", Locale.getDefault()).format(Date())
            repository.insertAnnouncement(Announcement(
                title = title, content = content, date = today,
                deptId = targetDeptId, semester = targetSemester, author = author, attachmentName = attachmentName
            ))
        }
    }

    fun deleteAnnouncement(id: Int) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
        }
    }

    // --- Leave Management ---
    fun submitLeaveRequest(startDate: String, endDate: String, reason: String, docName: String, requestType: String, targetFacultyIds: String = "", targetFacultyNames: String = "") {
        val student = _currentStudent.value ?: return
        viewModelScope.launch {
            repository.insertLeaveRequest(LeaveRequest(
                studentId = student.id, startDate = startDate, endDate = endDate,
                reason = reason, status = "Pending", documentPath = docName, requestType = requestType,
                targetFacultyIds = targetFacultyIds, targetFacultyNames = targetFacultyNames
            ))
        }
    }

    fun reviewLeaveRequest(request: LeaveRequest, status: String, comment: String) {
        viewModelScope.launch {
            val facultyName = _currentFaculty.value?.name ?: "Unknown Faculty"
            var newStatus = status
            var newApprovedBy = request.approvedBy
            var newComment = comment

            if (status == "Approved") {
                val approvers = request.approvedBy.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                if (!approvers.contains(facultyName)) {
                    approvers.add(facultyName)
                }
                newApprovedBy = approvers.joinToString(", ")
                
                val targets = request.targetFacultyIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val targetCount = targets.size
                
                if (targetCount > 0) {
                    if (approvers.size >= targetCount) {
                        newStatus = "Approved"
                        newComment = "Approved by all requested faculty: $newApprovedBy"
                    } else {
                        newStatus = "Pending (${approvers.size}/$targetCount Approved)"
                        newComment = "Approved by $facultyName. Waiting for other selected faculty approvals."
                    }
                } else {
                    if (approvers.size >= 2) {
                        newStatus = "Approved"
                        newComment = "Approved by: $newApprovedBy"
                    } else {
                        newStatus = "Pending"
                        newComment = "Approved by $facultyName. Waiting for additional approval."
                    }
                }
            } else if (status == "Rejected") {
                newStatus = "Rejected"
                newComment = "Rejected by $facultyName"
            }

            val updated = request.copy(status = newStatus, comments = newComment, approvedBy = newApprovedBy)
            repository.updateLeaveRequest(updated)

            // If approved, retroactively update attendance records for this student during the leave date range
            if (newStatus == "Approved") {
                val studentRecords = records.value.filter { it.studentId == request.studentId }
                studentRecords.forEach { record ->
                    val session = repository.getSessionById(record.sessionId)
                    if (session != null && isDateInRange(session.date, request.startDate, request.endDate)) {
                        repository.updateRecord(record.copy(status = "Approved Leave"))
                    }
                }
            }
        }
    }

    private fun isDateInRange(dateStr: String, startStr: String, endStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr)
            val start = sdf.parse(startStr)
            val end = sdf.parse(endStr)
            date in start..end
        } catch (e: Exception) {
            false
        }
    }

    // --- Settings CRUD ---
    fun updateSystemSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.insertSetting(key, value)
        }
    }

    // --- Attendance Mark Form Logic ---
    fun loadStudentsForMarking(deptId: Int, semester: Int, section: String, subjectId: Int, date: String, period: Int) {
        selectedDeptIdForMarking.value = deptId
        selectedSemesterForMarking.value = semester
        selectedSectionForMarking.value = section
        selectedSubjectIdForMarking.value = subjectId
        selectedDateForMarking.value = date
        selectedPeriodForMarking.value = period

        viewModelScope.launch {
            // Check if there is already an existing session for this exact slot to prevent duplicates
            val currentSessionsList = sessions.value
            val existingSession = currentSessionsList.firstOrNull {
                it.deptId == deptId && it.semester == semester && it.section == section &&
                it.subjectId == subjectId && it.date == date && it.period == period
            }

            if (existingSession != null) {
                isAttendanceLocked.value = existingSession.isLocked
                // Fetch existing records
                val existingRecords = repository.getRecordsForSession(existingSession.id).first()
                val classStudents = students.value.filter {
                    it.deptId == deptId && it.semester == semester && it.section == section
                }
                val states = classStudents.map { student ->
                    val rec = existingRecords.firstOrNull { it.studentId == student.id }
                    StudentAttendanceState(student, rec?.status ?: "Present")
                }
                _markingStateList.value = states
            } else {
                isAttendanceLocked.value = false
                val classStudents = students.value.filter {
                    it.deptId == deptId && it.semester == semester && it.section == section
                }
                val states = classStudents.map { StudentAttendanceState(it, "Present") }
                _markingStateList.value = states
            }
        }
    }

    fun updateStudentAttendanceState(studentId: Int, newStatus: String) {
        _markingStateList.value = _markingStateList.value.map {
            if (it.student.id == studentId) it.copy(status = newStatus) else it
        }
    }

    fun markAllPresent() {
        _markingStateList.value = _markingStateList.value.map { it.copy(status = "Present") }
    }

    fun submitAttendance(lockAfterSubmit: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            val deptId = selectedDeptIdForMarking.value
            val semester = selectedSemesterForMarking.value
            val section = selectedSectionForMarking.value
            val subjectId = selectedSubjectIdForMarking.value
            val date = selectedDateForMarking.value
            val period = selectedPeriodForMarking.value
            val facultyId = _currentFaculty.value?.id ?: 1
            val classroom = selectedClassroomForMarking.value

            val currentSessionsList = sessions.value
            var existingSession = currentSessionsList.firstOrNull {
                it.deptId == deptId && it.semester == semester && it.section == section &&
                it.subjectId == subjectId && it.date == date && it.period == period
            }

            val sessionId: Int
            if (existingSession != null) {
                sessionId = existingSession.id
                if (existingSession.isLocked) {
                    // Locked attendance cannot be rewritten directly unless overridden by admin
                    return@launch
                }
                if (lockAfterSubmit) {
                    existingSession = existingSession.copy(isLocked = true)
                    repository.updateSession(existingSession)
                    isAttendanceLocked.value = true
                }
            } else {
                val newSession = AttendanceSession(
                    deptId = deptId, semester = semester, section = section, subjectId = subjectId,
                    facultyId = facultyId, classroom = classroom, date = date, period = period, isLocked = lockAfterSubmit
                )
                sessionId = repository.insertSession(newSession).toInt()
                isAttendanceLocked.value = lockAfterSubmit
            }

            // Save records and trigger notifications for absent students
            val currentRecords = _markingStateList.value
            val subjectName = subjects.value.firstOrNull { it.id == subjectId }?.name ?: "Subject"

            currentRecords.forEach { state ->
                val existingRecord = repository.getRecordForStudentAndSession(sessionId, state.student.id)
                if (existingRecord != null) {
                    repository.updateRecord(existingRecord.copy(status = state.status))
                } else {
                    repository.insertRecord(AttendanceRecord(
                        sessionId = sessionId, studentId = state.student.id,
                        status = state.status, timestamp = System.currentTimeMillis()
                    ))
                }

                // SIMULATED AUTOMATED NOTIFICATION
                // Trigger an SMS if the status is "Absent"
                if (state.status == "Absent") {
                    sendSimulatedAbsentNotifications(state.student, subjectName, period, date)
                }
            }
            onComplete()
        }
    }

    private fun sendSimulatedAbsentNotifications(student: Student, subjectName: String, period: Int, date: String) {
        val schoolName = settings.value.firstOrNull { it.key == "school_name" }?.value ?: "G.R.T. Institute of Technology"
        
        // Parent Notification Message
        val parentMsg = "Alert from $schoolName: Your child ${student.name} (Roll ${student.rollNum}) was marked ABSENT today in $subjectName, Class Period $period on $date."
        // Student Notification Message
        val studentMsg = "Hi ${student.name}, you were marked ABSENT today in $subjectName, Period $period on $date. If this was an error, please request an attendance correction."

        val isTwilioConfigured = true // Simulated gateway status
        val channel = if (isTwilioConfigured) "SMS (Twilio)" else "Email (Nodemailer Fallback)"

        val logs = _smsNotificationsLog.value.toMutableList()
        
        // Log message for parent
        logs.add(0, SmsNotificationLogEntry(
            recipientName = student.parentName + " (Parent)",
            toPhone = student.parentPhone,
            message = parentMsg,
            channel = channel
        ))

        // Log message for student
        logs.add(0, SmsNotificationLogEntry(
            recipientName = student.name + " (Student)",
            toPhone = student.phone,
            message = studentMsg,
            channel = channel
        ))

        _smsNotificationsLog.value = logs
    }

    // --- QR Attendance Feature Simulation ---
    fun startQrSession(onTokenGenerated: (String) -> Unit) {
        viewModelScope.launch {
            val deptId = selectedDeptIdForMarking.value
            val semester = selectedSemesterForMarking.value
            val section = selectedSectionForMarking.value
            val subjectId = selectedSubjectIdForMarking.value
            val date = selectedDateForMarking.value
            val period = selectedPeriodForMarking.value
            val facultyId = _currentFaculty.value?.id ?: 1
            val classroom = selectedClassroomForMarking.value

            val token = "QR_ATT_" + UUID.randomUUID().toString().take(6).uppercase()
            val expiry = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes valid window

            val newSession = AttendanceSession(
                deptId = deptId, semester = semester, section = section, subjectId = subjectId,
                facultyId = facultyId, classroom = classroom, date = date, period = period,
                qrToken = token, qrExpiry = expiry, isLocked = false
            )

            repository.insertSession(newSession)
            onTokenGenerated(token)
        }
    }

    fun scanAndCheckInQr(token: String, studentLat: Double, studentLng: Double, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val session = repository.getSessionByQrToken(token)
            if (session == null) {
                onResult(false, "Invalid or unrecognized QR Code.")
                return@launch
            }

            if (System.currentTimeMillis() > session.qrExpiry) {
                onResult(false, "This QR Code has expired (active attendance window ended).")
                return@launch
            }

            val student = _currentStudent.value
            if (student == null) {
                onResult(false, "No active student session detected.")
                return@launch
            }

            // Verify if student matches the classroom section/dept
            if (student.deptId != session.deptId || student.semester != session.semester || student.section != session.section) {
                onResult(false, "You do not belong to the schedule for this active class.")
                return@launch
            }

            // GPS Presence Verification
            val classroomLat = 13.0827 // Target latitude for classroom
            val classroomLng = 80.2707 // Target longitude for classroom
            val distance = calculateDistanceInMeters(studentLat, studentLng, classroomLat, classroomLng)

            val isGpsEnabled = settings.value.firstOrNull { it.key == "enable_gps" }?.value == "true"
            if (isGpsEnabled && distance > 50.0) { // Classroom threshold of 50 meters
                onResult(false, "GPS verification failed. You are ${distance.toInt()}m away from the classroom.")
                return@launch
            }

            // Check if record already exists
            val existingRecord = repository.getRecordForStudentAndSession(session.id, student.id)
            if (existingRecord != null) {
                onResult(true, "Your attendance is already marked as ${existingRecord.status}.")
                return@launch
            }

            // Mark attendance as Present
            repository.insertRecord(AttendanceRecord(
                sessionId = session.id,
                studentId = student.id,
                status = "Present",
                timestamp = System.currentTimeMillis()
            ))

            onResult(true, "Attendance marked successfully! GPS Verification: PASSED (${distance.toInt()}m from target).")
        }
    }

    private fun calculateDistanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // Earth's radius in meters
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dphi = Math.toRadians(lat2 - lat1)
        val dlambda = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dphi / 2) * Math.sin(dphi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(dlambda / 2) * Math.sin(dlambda / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return r * c
    }

    // --- Student Request Attendance Correction ---
    fun submitCorrectionRequest(recordId: Int, reason: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val recs = records.value
            val record = recs.firstOrNull { it.id == recordId }
            if (record != null) {
                repository.updateRecord(record.copy(correctionReason = reason, correctionStatus = "Pending"))
                onComplete()
            }
        }
    }

    fun reviewCorrectionRequest(record: AttendanceRecord, approved: Boolean) {
        viewModelScope.launch {
            val newStatus = if (approved) "Present" else record.status
            val corrStatus = if (approved) "Approved" else "Rejected"
            repository.updateRecord(record.copy(status = newStatus, correctionStatus = corrStatus))
        }
    }

    // --- AI Smart Chatbot ---
    fun sendMessageToChatbot(text: String) {
        val currentMsgs = _chatbotMessages.value.toMutableList()
        currentMsgs.add(ChatbotMessage(text = text, sender = "User"))
        _chatbotMessages.value = currentMsgs

        _isChatbotLoading.value = true

        viewModelScope.launch {
            // Build Context Snapshot of current department database state
            val deptsList = departments.value.joinToString { "${it.name} (${it.code})" }
            val totalFacultyCount = faculty.value.size
            val totalStudentsCount = students.value.size
            
            // Generate summary of students below 75%
            val studentsList = students.value
            val recsList = records.value
            val lowAttendanceSummaryList = mutableListOf<String>()
            
            studentsList.forEach { s ->
                val studentRecs = recsList.filter { it.studentId == s.id }
                if (studentRecs.isNotEmpty()) {
                    val presentCount = studentRecs.count { it.status == "Present" || it.status == "Late" || it.status == "On Duty" || it.status == "Approved Leave" }
                    val pct = (presentCount.toFloat() / studentRecs.size.toFloat()) * 100
                    if (pct < 75.0) {
                        lowAttendanceSummaryList.add("${s.name} (ID: ${s.studentId}, Dept Code: ${s.deptId}, Attendance: ${pct.toInt()}%)")
                    }
                }
            }
            val lowAttendanceContextStr = if (lowAttendanceSummaryList.isNotEmpty()) {
                lowAttendanceSummaryList.joinToString("\n")
            } else {
                "None (All students are above the 75% requirement!)."
            }

            val pendingLeavesCount = leaveRequests.value.count { it.status == "Pending" }
            val holidaysStr = holidays.value.joinToString { "${it.name} on ${it.date}" }

            val systemContext = """
                You are the official AI Academic Advisor and Assistant for the Student Information System (SIS) at GRT Institute of Engineering and Technology.
                You support both English and Tamil (தமிழ்), communicating with clarity, warmth, and academic precision.
                You have real-time access to the local institution database. Here is the current state of the database:
                - Departments: $deptsList
                - Total Faculty Count: $totalFacultyCount
                - Total Enrolled Students: $totalStudentsCount
                - Students with Attendance Warning (<75%):
                $lowAttendanceContextStr
                - Active Student Leave Requests: $pendingLeavesCount
                - Upcoming Academic Calendar & Holidays: $holidaysStr
                - Active Academic Timetables and Sessions: ${sessions.value.size}

                The user of the Student Information System (Admin, Faculty, Parent, or Student) has asked: "$text".
                If the user writes in Tamil or asks in Tamil, reply in natural Tamil. If in English, reply in English (or bilingual if helpful for parents).
                Provide a complete, helpful, encouraging response regarding academic records, internal marks, attendance compliance, degree progress, fees, and leave letters.
            """.trimIndent()

            val response = withContext(Dispatchers.IO) {
                GeminiClient.generate(prompt = text, systemInstruction = systemContext)
            }

            val updatedMsgs = _chatbotMessages.value.toMutableList()
            updatedMsgs.add(ChatbotMessage(text = response, sender = "AI"))
            _chatbotMessages.value = updatedMsgs
            _isChatbotLoading.value = false
        }
    }

    // --- AI Attendance Predictor and Risk Forecaster ---
    fun predictStudentAttendanceRisk(studentId: Int) {
        _isPredictionLoading.value = true
        _predictionResult.value = null

        viewModelScope.launch {
            val student = repository.getStudentById(studentId)
            if (student == null) {
                _predictionResult.value = "Student not found."
                _isPredictionLoading.value = false
                return@launch
            }

            val studentRecs = records.value.filter { it.studentId == studentId }
            if (studentRecs.isEmpty()) {
                _predictionResult.value = "No historical attendance records available for ${student.name} to forecast trends."
                _isPredictionLoading.value = false
                return@launch
            }

            val totalClasses = studentRecs.size
            val presentCount = studentRecs.count { it.status == "Present" || it.status == "Late" || it.status == "On Duty" || it.status == "Approved Leave" }
            val currentPct = (presentCount.toFloat() / totalClasses.toFloat()) * 100
            val absentCount = studentRecs.count { it.status == "Absent" }
            val leavesCount = studentRecs.count { it.status == "Medical Leave" || it.status == "Approved Leave" }

            // Create contextual prompt for prediction
            val prompt = """
                Predict the final attendance percentage, risk category, and provide helpful advice for:
                Student: ${student.name}
                Current Attendance: ${currentPct.toInt()}% ($presentCount present out of $totalClasses classes)
                Absences: $absentCount
                Approved Leaves: $leavesCount
                
                Calculate risk (High Risk if under 75% and absent rate is high; Moderate Risk if 75-80%; Low Risk if >80%).
                Provide an automatic summarization including:
                1. Projected Final Semester Attendance Percentage based on the current run rate.
                2. Risk Level (Low/Medium/High).
                3. Concrete recommendations (e.g. "needs 5 consecutive classes", "submit medical documents").
                4. A friendly, motivational concluding tip.
                Format as a concise structured response.
            """.trimIndent()

            val result = withContext(Dispatchers.IO) {
                GeminiClient.generate(prompt = prompt, systemInstruction = "You are an expert AI Academic Advisor analyzing attendance risks.")
            }

            _predictionResult.value = result
            _isPredictionLoading.value = false
        }
    }

    // --- Database Seeding Logic ---
    private suspend fun seedDatabaseIfRequired() {
        val userCount = repository.getUserCount()
        if (userCount > 0) return // Already seeded

        // 1. Seed Departments
        val depts = listOf(
            Department(id = 1, name = "Computer Science & Engineering", code = "CSE"),
            Department(id = 2, name = "Information Technology", code = "IT"),
            Department(id = 3, name = "Electronics & Communication Engineering", code = "ECE"),
            Department(id = 4, name = "Mechanical Engineering", code = "MECH")
        )
        depts.forEach { repository.insertDepartment(it) }

        // 2. Seed Faculty
        val facs = listOf(
            Faculty(id = 1, name = "Dr. Alan Turing", email = "alan@college.edu", phone = "+1234567891", deptId = 1, designation = "HOD & Professor"),
            Faculty(id = 2, name = "Prof. Grace Hopper", email = "grace@college.edu", phone = "+1234567892", deptId = 2, designation = "Associate Professor"),
            Faculty(id = 3, name = "Dr. Claude Shannon", email = "claude@college.edu", phone = "+1234567893", deptId = 3, designation = "Professor")
        )
        facs.forEach { repository.insertFaculty(it) }

        // 3. Seed Students
        val studs = listOf(
            Student(
                id = 1, studentId = "CS23001", regNum = "CS-REG-23001", rollNum = "01",
                name = "Alice Smith", email = "alice@college.edu", phone = "+14085551111",
                deptId = 1, semester = 3, section = "A", parentName = "John Smith", parentPhone = "+14085559999",
                address = "123 Silicon Valley Road, San Jose, CA", bloodGroup = "O+", dob = "2005-04-12"
            ),
            Student(
                id = 2, studentId = "CS23002", regNum = "CS-REG-23002", rollNum = "02",
                name = "Bob Jones", email = "bob@college.edu", phone = "+14085552222",
                deptId = 1, semester = 3, section = "A", parentName = "Sarah Jones", parentPhone = "+14085558888",
                address = "456 Oakwood Circle, Sunnyvale, CA", bloodGroup = "A-", dob = "2005-09-25"
            ),
            Student(
                id = 3, studentId = "CS23003", regNum = "CS-REG-23003", rollNum = "03",
                name = "Charlie Brown", email = "charlie@college.edu", phone = "+14085553333",
                deptId = 1, semester = 3, section = "A", parentName = "Mary Brown", parentPhone = "+14085557777",
                address = "789 Redwood Boulevard, Cupertino, CA", bloodGroup = "B+", dob = "2005-11-05"
            ),
            Student(
                id = 4, studentId = "IT22001", regNum = "IT-REG-22001", rollNum = "01",
                name = "Diana Prince", email = "diana@college.edu", phone = "+14085554444",
                deptId = 2, semester = 5, section = "A", parentName = "Bruce Prince", parentPhone = "+14085556666",
                address = "10 Wondrous Avenue, Gotham, NY", bloodGroup = "AB+", dob = "2004-01-18"
            ),
            Student(
                id = 5, studentId = "IT22002", regNum = "IT-REG-22002", rollNum = "02",
                name = "Evan Wright", email = "evan@college.edu", phone = "+14085555555",
                deptId = 2, semester = 5, section = "A", parentName = "Helen Wright", parentPhone = "+14085550000",
                address = "22 Windy Lane, San Francisco, CA", bloodGroup = "O-", dob = "2004-06-30"
            )
        )
        studs.forEach { repository.insertStudent(it) }

        // 4. Seed Subjects
        val subs = listOf(
            Subject(id = 1, name = "Data Structures & Algorithms", code = "CS301", deptId = 1, semester = 3),
            Subject(id = 2, name = "Digital Logic & Computer Design", code = "CS302", deptId = 1, semester = 3),
            Subject(id = 3, name = "Discrete Mathematics", code = "CS303", deptId = 1, semester = 3),
            Subject(id = 4, name = "Database Management Systems", code = "IT501", deptId = 2, semester = 5),
            Subject(id = 5, name = "Software Engineering", code = "IT502", deptId = 2, semester = 5),
            Subject(id = 6, name = "Web Programming & Frameworks", code = "IT503", deptId = 2, semester = 5)
        )
        subs.forEach { repository.insertSubject(it) }

        // 5. Seed Users (Login Credentials)
        val users = listOf(
            User(id = 1, username = "admin@college.edu", passwordHash = "admin123", role = "Admin", associatedId = 0),
            User(id = 2, username = "alan@college.edu", passwordHash = "password123", role = "Faculty", associatedId = 1),
            User(id = 3, username = "grace@college.edu", passwordHash = "password123", role = "Faculty", associatedId = 2),
            User(id = 4, username = "alice@college.edu", passwordHash = "password123", role = "Student", associatedId = 1),
            User(id = 5, username = "bob@college.edu", passwordHash = "password123", role = "Student", associatedId = 2),
            User(id = 6, username = "charlie@college.edu", passwordHash = "password123", role = "Student", associatedId = 3),
            User(id = 7, username = "diana@college.edu", passwordHash = "password123", role = "Student", associatedId = 4),
            User(id = 8, username = "evan@college.edu", passwordHash = "password123", role = "Student", associatedId = 5)
        )
        users.forEach { repository.insertUser(it) }

        // 6. Seed Timetable schedules
        val tts = listOf(
            Timetable(id = 1, deptId = 1, semester = 3, section = "A", subjectId = 1, facultyId = 1, dayOfWeek = "Monday", period = 1, classroom = "Room 401", startTime = "09:00", endTime = "10:00"),
            Timetable(id = 2, deptId = 1, semester = 3, section = "A", subjectId = 2, facultyId = 1, dayOfWeek = "Monday", period = 2, classroom = "Room 401", startTime = "10:00", endTime = "11:00"),
            Timetable(id = 3, deptId = 1, semester = 3, section = "A", subjectId = 3, facultyId = 1, dayOfWeek = "Monday", period = 3, classroom = "Room 402", startTime = "11:15", endTime = "12:15"),
            Timetable(id = 4, deptId = 2, semester = 5, section = "A", subjectId = 4, facultyId = 2, dayOfWeek = "Monday", period = 1, classroom = "Room 503", startTime = "09:00", endTime = "10:00"),
            Timetable(id = 5, deptId = 2, semester = 5, section = "A", subjectId = 5, facultyId = 2, dayOfWeek = "Monday", period = 2, classroom = "Room 503", startTime = "10:00", endTime = "11:00"),
            Timetable(id = 6, deptId = 2, semester = 5, section = "A", subjectId = 6, facultyId = 2, dayOfWeek = "Monday", period = 3, classroom = "Room 504", startTime = "11:15", endTime = "12:15")
        )
        tts.forEach { repository.insertTimetable(it) }

        // 7. Seed Holidays
        val hols = listOf(
            Holiday(id = 1, name = "Independence Day", date = "2026-07-04", type = "National"),
            Holiday(id = 2, name = "Labor Day", date = "2026-09-07", type = "National"),
            Holiday(id = 3, name = "Mid-Term Academic Break", date = "2026-10-12", type = "Academic")
        )
        hols.forEach { repository.insertHoliday(it) }

        // 8. Seed Announcements
        val anns = listOf(
            Announcement(
                id = 1, title = "Welcome to the 2026 Academic Year!",
                content = "Welcome back, students and faculty! Classes commence officially on July 15th. Please ensure you have reviewed your timetables.",
                date = "2026-07-10", deptId = -1, semester = -1, author = "Admin Desk"
            ),
            Announcement(
                id = 2, title = "Data Structures Lab Guidelines",
                content = "All CSE Sem 3 students must bring their lab manuals and configured laptops with gcc/g++ compilers for Monday's sessions.",
                date = "2026-07-11", deptId = 1, semester = 3, author = "Dr. Alan Turing"
            )
        )
        anns.forEach { repository.insertAnnouncement(it) }

        // 9. Seed System Settings
        val sets = listOf(
            SystemSetting("school_name", "G.R.T. Institute of Technology"),
            SystemSetting("min_attendance_pct", "75"),
            SystemSetting("enable_qr", "true"),
            SystemSetting("enable_gps", "true"),
            SystemSetting("sms_template", "Alert: Your child {student_name} was marked ABSENT in {subject_name} (Period {period}) on {date}.")
        )
        sets.forEach { repository.insertSetting(it.key, it.value) }

        // 10. Seed historical Attendance sessions & records to make statistics beautiful
        val dates = listOf("2026-07-06", "2026-07-07", "2026-07-08", "2026-07-09", "2026-07-10")
        
        var sesId = 1
        dates.forEach { date ->
            // CSE Sem 3 Sessions (Data Structures (id 1) and Digital Logic (id 2))
            val cseSession1 = AttendanceSession(
                id = sesId, deptId = 1, semester = 3, section = "A", subjectId = 1, facultyId = 1,
                classroom = "Room 401", date = date, period = 1, isLocked = true
            )
            repository.insertSession(cseSession1)
            
            // Student 1 (Alice): present 100%
            repository.insertRecord(AttendanceRecord(sessionId = sesId, studentId = 1, status = "Present", timestamp = System.currentTimeMillis()))
            // Student 2 (Bob): present 80% (absent on 2026-07-08)
            val bobStatus = if (date == "2026-07-08") "Absent" else "Present"
            repository.insertRecord(AttendanceRecord(sessionId = sesId, studentId = 2, status = bobStatus, timestamp = System.currentTimeMillis()))
            // Student 3 (Charlie): low attendance (absent on 06, 08, 10 - 40% attendance!)
            val charlieStatus = if (date == "2026-07-06" || date == "2026-07-08" || date == "2026-07-10") "Absent" else "Present"
            repository.insertRecord(AttendanceRecord(sessionId = sesId, studentId = 3, status = charlieStatus, timestamp = System.currentTimeMillis()))

            sesId++

            val cseSession2 = AttendanceSession(
                id = sesId, deptId = 1, semester = 3, section = "A", subjectId = 2, facultyId = 1,
                classroom = "Room 401", date = date, period = 2, isLocked = true
            )
            repository.insertSession(cseSession2)
            repository.insertRecord(AttendanceRecord(sessionId = sesId, studentId = 1, status = "Present", timestamp = System.currentTimeMillis()))
            repository.insertRecord(AttendanceRecord(sessionId = sesId, studentId = 2, status = "Present", timestamp = System.currentTimeMillis()))
            repository.insertRecord(AttendanceRecord(sessionId = sesId, studentId = 3, status = if (date == "2026-07-07" || date == "2026-07-09") "Absent" else "Present", timestamp = System.currentTimeMillis()))

            sesId++

            // IT Sem 5 Session (Database Management Systems (id 4))
            val itSession = AttendanceSession(
                id = sesId, deptId = 2, semester = 5, section = "A", subjectId = 4, facultyId = 2,
                classroom = "Room 503", date = date, period = 1, isLocked = true
            )
            repository.insertSession(itSession)
            // Student 4 (Diana): present 100%
            repository.insertRecord(AttendanceRecord(sessionId = sesId, studentId = 4, status = "Present", timestamp = System.currentTimeMillis()))
            // Student 5 (Evan): present 80%
            val evanStatus = if (date == "2026-07-10") "Absent" else "Present"
            repository.insertRecord(AttendanceRecord(sessionId = sesId, studentId = 5, status = evanStatus, timestamp = System.currentTimeMillis()))

            sesId++
        }
    }
}

class AppViewModelFactory(private val application: Application, private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
