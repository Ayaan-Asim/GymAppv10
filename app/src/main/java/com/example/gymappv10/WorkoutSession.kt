package com.example.gymappv10

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey val sessionId: String = UUID.randomUUID().toString(),
    val date: String, // still keep this

    val totalDuration: Int,
    val totalCalories: Int,
    val timestamp: Long = System.currentTimeMillis()
)
