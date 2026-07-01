package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.GardenApp
import com.example.ui.GardenViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: GardenViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
      val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
      val systemTheme = isSystemInDarkTheme()
      val darkTheme = isDarkMode ?: systemTheme
      
      MyApplicationTheme(darkTheme = darkTheme) {
        GardenApp(viewModel = viewModel)
      }
    }
  }
}

