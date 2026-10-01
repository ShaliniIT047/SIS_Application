package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- Users ---
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: Int)

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM users WHERE role = :role AND associatedId = :associatedId LIMIT 1")
    suspend fun getUserByRoleAndId(role: String, associatedId: Int): User?


    // --- Departments ---
    @Query("SELECT * FROM departments ORDER BY name ASC")
    fun getAllDepartments(): Flow<List<Department>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDepartment(department: Department): Long

    @Query("DELETE FROM departments WHERE id = :id")
    suspend fun deleteDepartment(id: Int)


    // --- Subjects ---
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE deptId = :deptId AND semester = :semester")
    fun getSubjectsForClass(deptId: Int, semester: Int): Flow<List<Subject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubject(id: Int)


    // --- Faculty ---
    @Query("SELECT * FROM faculty ORDER BY name ASC")
    fun getAllFaculty(): Flow<List<Faculty>>

    @Query("SELECT * FROM faculty WHERE id = :id")
    suspend fun getFacultyById(id: Int): Faculty?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaculty(faculty: Faculty): Long

    @Query("DELETE FROM faculty WHERE id = :id")
    suspend fun deleteFaculty(id: Int)


    // --- Students ---
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: Int): Student?

    @Query("SELECT * FROM students WHERE deptId = :deptId AND semester = :semester AND section = :section ORDER BY rollNum ASC")
    fun getStudentsForClass(deptId: Int, semester: Int, section: String): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudent(id: Int)


    // --- Timetables ---
    @Query("SELECT * FROM timetables")
    fun getAllTimetables(): Flow<List<Timetable>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetable(timetable: Timetable): Long

    @Query("DELETE FROM timetables WHERE id = :id")
    suspend fun deleteTimetable(id: Int)

    @Query("SELECT * FROM timetables WHERE id = :id LIMIT 1")
    suspend fun getTimetableById(id: Int): Timetable?


    // --- Holidays ---
    @Query("SELECT * FROM holidays ORDER BY date ASC")
    fun getAllHolidays(): Flow<List<Holiday>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoliday(holiday: Holiday): Long

    @Query("DELETE FROM holidays WHERE id = :id")
    suspend fun deleteHoliday(id: Int)


    // --- Announcements ---
    @Query("SELECT * FROM announcements ORDER BY date DESC, id DESC")
    fun getAllAnnouncements(): Flow<List<Announcement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: Announcement): Long

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: Int)


    // --- Leave Requests ---
    @Query("SELECT * FROM leave_requests ORDER BY id DESC")
    fun getAllLeaveRequests(): Flow<List<LeaveRequest>>

    @Query("SELECT * FROM leave_requests WHERE studentId = :studentId ORDER BY id DESC")
    fun getLeaveRequestsForStudent(studentId: Int): Flow<List<LeaveRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaveRequest(leaveRequest: LeaveRequest): Long

    @Update
    suspend fun updateLeaveRequest(leaveRequest: LeaveRequest)

    @Query("DELETE FROM leave_requests WHERE id = :id")
    suspend fun deleteLeaveRequest(id: Int)


    // --- Attendance Sessions ---
    @Query("SELECT * FROM attendance_sessions ORDER BY date DESC, period DESC")
    fun getAllSessions(): Flow<List<AttendanceSession>>

    @Query("SELECT * FROM attendance_sessions WHERE id = :id")
    suspend fun getSessionById(id: Int): AttendanceSession?

    @Query("SELECT * FROM attendance_sessions WHERE qrToken = :qrToken LIMIT 1")
    suspend fun getSessionByQrToken(qrToken: String): AttendanceSession?

    @Query("SELECT * FROM attendance_sessions WHERE date = :date AND subjectId = :subjectId AND period = :period LIMIT 1")
    suspend fun getSessionBySubjectAndDateAndPeriod(date: String, subjectId: Int, period: Int): AttendanceSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AttendanceSession): Long

    @Update
    suspend fun updateSession(session: AttendanceSession)

    @Query("DELETE FROM attendance_sessions WHERE id = :id")
    suspend fun deleteSession(id: Int)


    // --- Attendance Records ---
    @Query("SELECT * FROM attendance_records")
    fun getAllRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId")
    fun getRecordsForSession(sessionId: Int): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId")
    fun getRecordsForStudent(studentId: Int): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId AND studentId = :studentId LIMIT 1")
    suspend fun getRecordForStudentAndSession(sessionId: Int, studentId: Int): AttendanceRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecord>)

    @Update
    suspend fun updateRecord(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteRecord(id: Int)


    // --- System Settings ---
    @Query("SELECT * FROM system_settings")
    fun getAllSettings(): Flow<List<SystemSetting>>

    @Query("SELECT value FROM system_settings WHERE key = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SystemSetting)
}
