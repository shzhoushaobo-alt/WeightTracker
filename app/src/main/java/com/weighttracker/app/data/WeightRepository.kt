package com.weighttracker.app.data

import kotlinx.coroutines.flow.Flow

class WeightRepository(private val dao: WeightDao) {
    fun observeRange(startDayMillis: Long, endDayMillis: Long): Flow<List<WeightRecord>> =
        dao.observeRange(startDayMillis, endDayMillis)

    suspend fun getLatest(): WeightRecord? = dao.getLatest()

    suspend fun getByDay(dayMillis: Long): WeightRecord? = dao.getByDay(dayMillis)

    suspend fun upsert(record: WeightRecord) {
        dao.insert(record)
    }
}
