package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.AppViewModel
import com.example.ui.AppViewModelFactory
import com.example.ui.Screen
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize DB and Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = AppRepository(database.appDao())

        // 2. Setup ViewModel Factory
        val factory = AppViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[AppViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 3. Collect active route reactively
                    val currentScreen = viewModel.currentScreen.collectAsState()

                    // 4. Custom Animated Route Switcher for highly fluid UI animations
                    AnimatedContent(
                        targetState = currentScreen.value,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "MainScreenNavigation"
                    ) { screen ->
                        when (screen) {
                            Screen.Home -> HomeScreen(viewModel = viewModel)
                            Screen.Login -> LoginScreen(viewModel = viewModel)
                            Screen.AdminDashboard -> AdminDashboardScreen(viewModel = viewModel)
                            Screen.FacultyDashboard -> FacultyDashboardScreen(viewModel = viewModel)
                            Screen.FacultyMarkAttendance -> FacultyMarkAttendanceScreen(viewModel = viewModel)
                            Screen.StudentDashboard -> StudentDashboardScreen(viewModel = viewModel)
                            Screen.StudentQRAttendance -> StudentQRAttendanceScreen(viewModel = viewModel)
                            Screen.Chatbot -> ChatbotScreen(viewModel = viewModel)
                            else -> {} // Cover sub-tabs handled internally
                        }
                    }
                }
            }
        }
    }
}
