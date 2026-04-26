package com.weighttracker.app

import android.app.Application
import com.weighttracker.app.data.UserPreferences
import com.weighttracker.app.data.WeightDatabase
import com.weighttracker.app.data.WeightRepository

class WeightApplication : Application() {
    val database by lazy { WeightDatabase.getInstance(this) }
    val repository by lazy { WeightRepository(database.weightDao()) }
    val userPreferences by lazy { UserPreferences(this) }
}
