package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val passwordHash: String,
    val role: String, // "Admin", "Faculty", "Student"
    val associatedId: Int // Links to Faculty.id or Student.id
)

@Entity(tableName = "departments")
data class Department(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val code: String
)

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val code: String,
    val deptId: Int,
    val semester: Int
)

@Entity(tableName = "faculty")
data class Faculty(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val phone: String,
    val deptId: Int,
    val designation: String,
    val photoUrl: String = ""
)

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val regNum: String,
    val rollNum: String,
    val name: String,
    val email: String,
    val phone: String,
    val deptId: Int,
    val semester: Int,
    val section: String, // "A", "B", "C"
    val parentName: String,
    val parentPhone: String,
    val address: String,
    val bloodGroup: String,
    val dob: String,
    val photoUrl: String = ""
)

@Entity(tableName = "timetables")
data class Timetable(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deptId: Int,
    val semester: Int,
    val section: String,
    val subjectId: Int,
    val facultyId: Int,
    val dayOfWeek: String, // "Monday", "Tuesday", etc.
    val period: Int, // 1, 2, 3, 4, 5, 6, 7, 8
    val classroom: String,
    val startTime: String = "09:00",
    val endTime: String = "10:00"
)

@Entity(tableName = "holidays")
data class Holiday(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val date: String, // "YYYY-MM-DD"
    val type: String // "National", "Academic", "Local"
)

@Entity(tableName = "announcements")
data class Announcement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val date: String, // "YYYY-MM-DD"
    val deptId: Int, // -1 for all
    val semester: Int, // -1 for all
    val author: String,
    val attachmentName: String = ""
)

@Entity(tableName = "leave_requests")
data class LeaveRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val startDate: String, // "YYYY-MM-DD"
    val endDate: String, // "YYYY-MM-DD"
    val reason: String,
    val status: String, // "Pending", "Approved", "Rejected"
    val comments: String = "",
    val documentPath: String = "",
    val requestType: String = "Leave Letter",
    val approvedBy: String = "", // Comma-separated names of faculty who approved (requires multiple)
    val targetFacultyIds: String = "", // Comma-separated faculty IDs requested for approval
    val targetFacultyNames: String = "" // Comma-separated faculty names requested for approval
)

@Entity(tableName = "attendance_sessions")
data class AttendanceSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deptId: Int,
    val semester: Int,
    val section: String,
    val subjectId: Int,
    val facultyId: Int,
    val classroom: String,
    val date: String, // "YYYY-MM-DD"
    val period: Int,
    val qrToken: String = "",
    val qrExpiry: Long = 0,
    val isLocked: Boolean = false
)

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val studentId: Int,
    val status: String, // "Present", "Absent", "Late", "On Duty", "Medical Leave", "Approved Leave"
    val timestamp: Long,
    val correctionReason: String = "",
    val correctionStatus: String = "None" // "None", "Pending", "Approved", "Rejected"
)

@Entity(tableName = "system_settings")
data class SystemSetting(
    @PrimaryKey val key: String,
    val value: String
)
