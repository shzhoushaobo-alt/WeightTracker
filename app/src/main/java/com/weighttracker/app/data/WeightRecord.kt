package com.weighttracker.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weight_records",
    indices = [Index(value = ["dateMillis"], unique = true)],
)
data class WeightRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Local day start epoch millis */
    val dateMillis: Long,
    /** Always stored in kilograms, 2 decimal precision in app logic */
    val weightKg: Double,
)
