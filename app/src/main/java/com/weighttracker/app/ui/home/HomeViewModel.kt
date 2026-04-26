package com.weighttracker.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.weighttracker.app.WeightApplication
import com.weighttracker.app.data.UserPreferences
import com.weighttracker.app.data.WeightRecord
import com.weighttracker.app.data.WeightRepository
import com.weighttracker.app.util.DisplayUnit
import com.weighttracker.app.util.startOfDayMillis
import com.weighttracker.app.util.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(
    private val repository: WeightRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {
    sealed interface DateFilter {
        data object Last7 : DateFilter
        data object Last30 : DateFilter
        data class Custom(val start: LocalDate, val end: LocalDate) : DateFilter
    }

    private val _filter = MutableStateFlow<DateFilter>(DateFilter.Last7)
    val filter: StateFlow<DateFilter> = _filter

    val displayUnit: StateFlow<DisplayUnit> = userPreferences.displayUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DisplayUnit.KG)

    val records: StateFlow<List<WeightRecord>> = _filter
        .flatMapLatest { f ->
            val (start, end) = f.toDayRange()
            repository.observeRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(filter: DateFilter) {
        _filter.value = filter
    }

    fun setCustomRange(start: LocalDate, end: LocalDate) {
        val s = if (start.isAfter(end)) end else start
        val e = if (end.isBefore(start)) start else end
        _filter.value = DateFilter.Custom(s, e)
    }

    fun toggleUnit() {
        viewModelScope.launch {
            val cur = userPreferences.displayUnit.first()
            val next = if (cur == DisplayUnit.KG) DisplayUnit.JIN else DisplayUnit.KG
            userPreferences.setDisplayUnit(next)
        }
    }

    suspend fun importRecords(records: List<WeightRecord>) {
        records.forEach { repository.upsert(it) }
    }

    private fun DateFilter.toDayRange(): Pair<Long, Long> {
        val endDay = today()
        return when (this) {
            DateFilter.Last7 -> {
                val start = endDay.minusDays(6)
                start.startOfDayMillis() to endDay.startOfDayMillis()
            }
            DateFilter.Last30 -> {
                val start = endDay.minusDays(29)
                start.startOfDayMillis() to endDay.startOfDayMillis()
            }
            is DateFilter.Custom -> start.startOfDayMillis() to end.startOfDayMillis()
        }
    }

    companion object {
        fun factory(app: WeightApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(HomeViewModel::class.java))
                    return HomeViewModel(app.repository, app.userPreferences) as T
                }
            }
    }
}
