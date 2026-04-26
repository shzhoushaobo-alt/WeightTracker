package com.weighttracker.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.weighttracker.app.util.DisplayUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "prefs")

class UserPreferences(private val context: Context) {
    private val unitKey = stringPreferencesKey("display_unit")

    val displayUnit: Flow<DisplayUnit> = context.dataStore.data.map { p ->
        when (p[unitKey]) {
            DisplayUnit.JIN.name -> DisplayUnit.JIN
            else -> DisplayUnit.KG
        }
    }

    suspend fun setDisplayUnit(unit: DisplayUnit) {
        context.dataStore.edit { it[unitKey] = unit.name }
    }
}
