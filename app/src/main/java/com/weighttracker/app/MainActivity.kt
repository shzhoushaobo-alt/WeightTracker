package com.weighttracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.weighttracker.app.ui.add.AddRecordScreen
import com.weighttracker.app.ui.home.HomeScreen
import com.weighttracker.app.ui.theme.WeightTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeightApp()
        }
    }
}

@Composable
private fun WeightApp() {
    WeightTrackerTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(onAdd = { navController.navigate("add") })
            }
            composable("add") {
                AddRecordScreen(onDone = { navController.popBackStack() })
            }
        }
    }
}
