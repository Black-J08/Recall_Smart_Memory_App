package com.smartmemory.recall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.smartmemory.recall.ui.MainScreen
import com.smartmemory.recall.ui.settings.SettingsViewModel
import com.smartmemory.recall.ui.theme.RecallTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            
            RecallTheme(darkTheme = isDarkMode) {
                MainScreen()
            }
        }
    }
}