package com.example.gymappv10

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_entries",
    foreignKeys = [ForeignKey(
        entity = WorkoutSession::class,
        parentColumns = ["sessionId"],     // ✅ Referencing real PK now
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]         // ✅ Required for foreign key
)
data class WorkoutEntry(
    @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
    val sessionId: String,                 // ✅ FK to WorkoutSession
    val exerciseId: String,
    val durationMin: Int,
    val reps: Int?,
    val calories: Int
)
