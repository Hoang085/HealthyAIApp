package com.healthyai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.healthyai.app.presentation.navigation.AppNavigation
import com.healthyai.app.presentation.ui.theme.HealthyAITheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Màn hình duy nhất (Single Activity Architecture).
 * Toàn bộ UI được xây dựng bằng Jetpack Compose bên trong.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Hiển thị tràn màn hình (modern Android design)
        setContent {
            HealthyAITheme {
                AppNavigation()
            }
        }
    }
}
