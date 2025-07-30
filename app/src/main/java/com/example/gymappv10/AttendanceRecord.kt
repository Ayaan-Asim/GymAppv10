package com.example.gymappv10

data class AttendanceRecord(
    val date: String = "",
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    val durationMinutes: Long? = null
)