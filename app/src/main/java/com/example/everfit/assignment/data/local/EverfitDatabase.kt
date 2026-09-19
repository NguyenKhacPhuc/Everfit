package com.example.everfit.assignment.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.everfit.assignment.data.local.dao.CompletionDao
import com.example.everfit.assignment.data.local.dao.WorkoutDao
import com.example.everfit.assignment.data.local.entity.CompletionOverrideEntity
import com.example.everfit.assignment.data.local.entity.WorkoutAssignmentEntity

@Database(
    entities = [WorkoutAssignmentEntity::class, CompletionOverrideEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class EverfitDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun completionDao(): CompletionDao

    companion object {
        fun build(context: Context): EverfitDatabase =
            Room.databaseBuilder(context, EverfitDatabase::class.java, "everfit.db").build()
    }
}
