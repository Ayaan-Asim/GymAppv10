package com.example.gymappv10

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Exercise::class, WorkoutEntry::class, WorkoutSession::class],
    version = 1
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun entryDao(): WorkoutEntryDao
    abstract fun sessionDao(): WorkoutSessionDao

    companion object {
        private var INSTANCE: WorkoutDatabase? = null
        fun getInstance(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
