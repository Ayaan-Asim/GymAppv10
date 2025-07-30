package com.example.gymappv10

import androidx.room.*

@Dao
interface WeightLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WeightLog)

    @Query("SELECT * FROM weight_log ORDER BY date ASC")
    suspend fun getAll(): List<WeightLog>
}
