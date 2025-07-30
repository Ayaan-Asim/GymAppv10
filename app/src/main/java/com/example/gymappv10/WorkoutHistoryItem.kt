package com.example.gymappv10

sealed class WorkoutHistoryItem {
    data class DateHeader(val date: String) : WorkoutHistoryItem()
    data class SessionDetail(val session: WorkoutSession, val entries: List<WorkoutEntry>) : WorkoutHistoryItem()
}
