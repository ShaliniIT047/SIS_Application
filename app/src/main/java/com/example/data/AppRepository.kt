package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {

    // --- Users ---
    suspend fun getUserByUsername(username: String): User? = appDao.getUserByUsername(username)
    suspend fun getUserCount(): Int = appDao.getUserCount()
    suspend fun insertUser(user: User): Long = appDao.insertUser(user)
    suspend fun deleteUserById(id: Int) = appDao.deleteUserById(id)
    suspend fun updateUser(user: User) = appDao.updateUser(user)
    suspend fun getUserByRoleAndId(role: String, associatedId: Int): User? = appDao.getUserByRoleAndId(role, associatedId)

    // --- Departments ---
    val allDepartments: Flow<List<Department>> = appDao.getAllDepartments()
    suspend fun insertDepartment(department: Department): Long = appDao.insertDepartment(department)
    suspend fun deleteDepartment(id: Int) = appDao.deleteDepartment(id)

    // --- Subjects ---
    val allSubjects: Flow<List<Subject>> = appDao.getAllSubjects()
    fun getSubjectsForClass(deptId: Int, semester: Int): Flow<List<Subject>> = appDao.getSubjectsForClass(deptId, semester)
    suspend fun insertSubject(subject: Subject): Long = appDao.insertSubject(subject)
    suspend fun deleteSubject(id: Int) = appDao.deleteSubject(id)

    // --- Faculty ---
    val allFaculty: Flow<List<Faculty>> = appDao.getAllFaculty()
    suspend fun getFacultyById(id: Int): Faculty? = appDao.getFacultyById(id)
    suspend fun insertFaculty(faculty: Faculty): Long = appDao.insertFaculty(faculty)
    suspend fun deleteFaculty(id: Int) = appDao.deleteFaculty(id)

    // --- Students ---
    val allStudents: Flow<List<Student>> = appDao.getAllStudents()
    suspend fun getStudentById(id: Int): Student? = appDao.getStudentById(id)
    fun getStudentsForClass(deptId: Int, semester: Int, section: String): Flow<List<Student>> = 
        appDao.getStudentsForClass(deptId, semester, section)
    suspend fun insertStudent(student: Student): Long = appDao.insertStudent(student)
    suspend fun deleteStudent(id: Int) = appDao.deleteStudent(id)

    // --- Timetables ---
    val allTimetables: Flow<List<Timetable>> = appDao.getAllTimetables()
    suspend fun insertTimetable(timetable: Timetable): Long = appDao.insertTimetable(timetable)
    suspend fun deleteTimetable(id: Int) = appDao.deleteTimetable(id)
    suspend fun getTimetableById(id: Int): Timetable? = appDao.getTimetableById(id)

    // --- Holidays ---
    val allHolidays: Flow<List<Holiday>> = appDao.getAllHolidays()
    suspend fun insertHoliday(holiday: Holiday): Long = appDao.insertHoliday(holiday)
    suspend fun deleteHoliday(id: Int) = appDao.deleteHoliday(id)

    // --- Announcements ---
    val allAnnouncements: Flow<List<Announcement>> = appDao.getAllAnnouncements()
    suspend fun insertAnnouncement(announcement: Announcement): Long = appDao.insertAnnouncement(announcement)
    suspend fun deleteAnnouncement(id: Int) = appDao.deleteAnnouncement(id)

    // --- Leave Requests ---
    val allLeaveRequests: Flow<List<LeaveRequest>> = appDao.getAllLeaveRequests()
    fun getLeaveRequestsForStudent(studentId: Int): Flow<List<LeaveRequest>> = appDao.getLeaveRequestsForStudent(studentId)
    suspend fun insertLeaveRequest(leaveRequest: LeaveRequest): Long = appDao.insertLeaveRequest(leaveRequest)
    suspend fun updateLeaveRequest(leaveRequest: LeaveRequest) = appDao.updateLeaveRequest(leaveRequest)
    suspend fun deleteLeaveRequest(id: Int) = appDao.deleteLeaveRequest(id)

    // --- Attendance Sessions ---
    val allSessions: Flow<List<AttendanceSession>> = appDao.getAllSessions()
    suspend fun getSessionById(id: Int): AttendanceSession? = appDao.getSessionById(id)
    suspend fun getSessionByQrToken(qrToken: String): AttendanceSession? = appDao.getSessionByQrToken(qrToken)
    suspend fun getSessionBySubjectAndDateAndPeriod(date: String, subjectId: Int, period: Int): AttendanceSession? =
        appDao.getSessionBySubjectAndDateAndPeriod(date, subjectId, period)
    suspend fun insertSession(session: AttendanceSession): Long = appDao.insertSession(session)
    suspend fun updateSession(session: AttendanceSession) = appDao.updateSession(session)
    suspend fun deleteSession(id: Int) = appDao.deleteSession(id)

    // --- Attendance Records ---
    val allRecords: Flow<List<AttendanceRecord>> = appDao.getAllRecords()
    fun getRecordsForSession(sessionId: Int): Flow<List<AttendanceRecord>> = appDao.getRecordsForSession(sessionId)
    fun getRecordsForStudent(studentId: Int): Flow<List<AttendanceRecord>> = appDao.getRecordsForStudent(studentId)
    suspend fun getRecordForStudentAndSession(sessionId: Int, studentId: Int): AttendanceRecord? = 
        appDao.getRecordForStudentAndSession(sessionId, studentId)
    suspend fun insertRecord(record: AttendanceRecord): Long = appDao.insertRecord(record)
    suspend fun insertRecords(records: List<AttendanceRecord>) = appDao.insertRecords(records)
    suspend fun updateRecord(record: AttendanceRecord) = appDao.updateRecord(record)
    suspend fun deleteRecord(id: Int) = appDao.deleteRecord(id)

    // --- System Settings ---
    val allSettings: Flow<List<SystemSetting>> = appDao.getAllSettings()
    suspend fun getSetting(key: String): String? = appDao.getSetting(key)
    suspend fun insertSetting(key: String, value: String) = appDao.insertSetting(SystemSetting(key, value))
}
