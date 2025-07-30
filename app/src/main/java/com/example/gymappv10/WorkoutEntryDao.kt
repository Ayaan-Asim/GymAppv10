package com.example.gymappv10

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WorkoutEntryDao {
    @Insert
    suspend fun insertAll(entries: List<WorkoutEntry>)

    @Query("SELECT * FROM workout_entries WHERE sessionId = :sessionId")
    suspend fun getBySessionId(sessionId: String): List<WorkoutEntry>

    @Query("""
        SELECT exerciseId, MAX(reps) as maxReps
        FROM workout_entries
        WHERE reps IS NOT NULL
        GROUP BY exerciseId
    """)
    suspend fun getPersonalRecords(): List<PRResult>
}
