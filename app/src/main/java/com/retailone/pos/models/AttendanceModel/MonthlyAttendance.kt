package com.retailone.pos.models.AttendanceModel

data class MonthlyAttendance(
    val all_login_data: List<AllLoginData>,
    val month: String,
    val total_attendance: Int,
    val year: Int
)