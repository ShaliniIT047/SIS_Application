package com.example.ui

sealed class Screen {
    object Home : Screen()
    object Login : Screen()
    
    // Admin Screens
    object AdminDashboard : Screen()
    object AdminManageDepts : Screen()
    object AdminManageSubjects : Screen()
    object AdminManageFaculty : Screen()
    object AdminManageStudents : Screen()
    object AdminManageTimetable : Screen()
    object AdminManageHolidays : Screen()
    object AdminSettings : Screen()
    
    // Faculty Screens
    object FacultyDashboard : Screen()
    object FacultyMarkAttendance : Screen()
    object FacultyAttendanceHistory : Screen()
    object FacultyLeaveRequests : Screen()
    
    // Student Screens
    object StudentDashboard : Screen()
    object StudentLeaveRequest : Screen()
    object StudentQRAttendance : Screen()
    
    // Common Screens
    object Chatbot : Screen()
}
