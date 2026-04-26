package com.weighttracker.app.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.weighttracker.app.WeightApplication
import com.weighttracker.app.data.UserPreferences
import com.weighttracker.app.data.WeightRecord
import com.weighttracker.app.data.WeightRepository
import com.weighttracker.app.util.DisplayUnit
import com.weighttracker.app.util.jinToKg
import com.weighttracker.app.util.parseWeightInput
import com.weighttracker.app.util.round2
import com.weighttracker.app.util.startOfDayMillis
import com.weighttracker.app.util.toDisplayString
import com.weighttracker.app.util.toDisplayValue
import com.weighttracker.app.util.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface SaveOutcome {
    data object Success : SaveOutcome
    data object NeedsReplaceConfirm : SaveOutcome
    data class Error(val message: String) : SaveOutcome
}

class AddViewModel(
    private val repository: WeightRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {
    private val _date = MutableStateFlow(today())
    val selectedDate: StateFlow<LocalDate> = _date.asStateFlow()

    private val _weightText = MutableStateFlow("")
    val weightText: StateFlow<String> = _weightText.asStateFlow()

    val displayUnit: StateFlow<DisplayUnit> = userPreferences.displayUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DisplayUnit.KG)

    init {
        refreshDefaults()
    }

    fun refreshDefaults() {
        viewModelScope.launch {
            val unit = userPreferences.displayUnit.first()
            val latest = repository.getLatest()
            val defaultKg = latest?.weightKg ?: 60.0
            _weightText.value = defaultKg.toDisplayValue(unit).toDisplayString()
            _date.value = today()
        }
    }

    fun setDate(d: LocalDate) {
        _date.value = d
    }

    fun setWeightText(t: String) {
        _weightText.value = t
    }

    fun adjustBy(deltaInDisplayUnit: Double) {
        viewModelScope.launch {
            val unit = userPreferences.displayUnit.first()
            val kg = parseWeightInput(_weightText.value, unit) ?: return@launch
            val display = kg.toDisplayValue(unit)
            val nextDisplay = (display + deltaInDisplayUnit).round2()
            val nextKg = when (unit) {
                DisplayUnit.KG -> nextDisplay
                DisplayUnit.JIN -> nextDisplay.jinToKg()
            }.round2()
            _weightText.value = nextKg.toDisplayValue(unit).toDisplayString()
        }
    }

    suspend fun save(allowReplace: Boolean): SaveOutcome {
        val unit = userPreferences.displayUnit.first()
        val kg = parseWeightInput(_weightText.value, unit)
            ?: return SaveOutcome.Error("请输入有效体重（两位小数）")
        if (kg <= 0 || kg > 500) return SaveOutcome.Error("体重超出合理范围")

        val day = _date.value.startOfDayMillis()
        val existing = repository.getByDay(day)
        if (existing != null && !allowReplace) return SaveOutcome.NeedsReplaceConfirm

        repository.upsert(WeightRecord(dateMillis = day, weightKg = kg))
        return SaveOutcome.Success
    }

    companion object {
        fun factory(app: WeightApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(AddViewModel::class.java))
                    return AddViewModel(app.repository, app.userPreferences) as T
                }
            }
    }
}
