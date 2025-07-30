package com.example.gymappv10

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_log")
data class WeightLog(
    @PrimaryKey val date: String,  // format "dd-MM-yyyy"
    val weight: Float
)
