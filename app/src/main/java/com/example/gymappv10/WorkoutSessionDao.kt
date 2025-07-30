package com.example.gymappv10

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkoutSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSession)

    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    suspend fun getAll(): List<WorkoutSession>

    @Query("SELECT * FROM workout_sessions ORDER BY date ASC")
    suspend fun getAllSortedByDate(): List<WorkoutSession> // ✅ For line chart

    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC LIMIT 3")
    suspend fun getLastThree(): List<WorkoutSession>

    @Query("""
        SELECT date, 
               SUM(totalDuration) AS totalDuration, 
               SUM(totalCalories) AS totalCalories 
        FROM workout_sessions 
        GROUP BY date 
        ORDER BY MAX(timestamp) DESC 
        LIMIT 3
    """)
    suspend fun getLastThreeDaysSummary(): List<DailyWorkoutSummary>

    @Query("""
    SELECT SUM(totalCalories) FROM workout_sessions
    WHERE strftime('%m', datetime(timestamp / 1000, 'unixepoch')) = :month
      AND strftime('%Y', datetime(timestamp / 1000, 'unixepoch')) = :year
""")
    suspend fun getTotalCaloriesThisMonth(month: String, year: String): Int?

}
