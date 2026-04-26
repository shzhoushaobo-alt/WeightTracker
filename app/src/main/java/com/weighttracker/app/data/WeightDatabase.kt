package com.weighttracker.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WeightRecord::class], version = 1, exportSchema = false)
abstract class WeightDatabase : RoomDatabase() {
    abstract fun weightDao(): WeightDao

    companion object {
        fun getInstance(context: Context): WeightDatabase =
            Room.databaseBuilder(context, WeightDatabase::class.java, "weight.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
