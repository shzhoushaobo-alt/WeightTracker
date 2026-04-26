package com.weighttracker.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Query(
        "SELECT * FROM weight_records WHERE dateMillis >= :startDayMillis AND dateMillis <= :endDayMillis ORDER BY dateMillis ASC",
    )
    fun observeRange(startDayMillis: Long, endDayMillis: Long): Flow<List<WeightRecord>>

    @Query("SELECT * FROM weight_records ORDER BY dateMillis DESC LIMIT 1")
    suspend fun getLatest(): WeightRecord?

    @Query("SELECT * FROM weight_records WHERE dateMillis = :dayMillis LIMIT 1")
    suspend fun getByDay(dayMillis: Long): WeightRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: WeightRecord): Long
}
