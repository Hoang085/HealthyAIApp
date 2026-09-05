package com.healthyai.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home     : Screen("home",     "Trang chủ",  Icons.Filled.Home)
    object Habits   : Screen("habits",   "Thói quen",  Icons.Filled.CheckCircle)
    object Camera   : Screen("camera",   "Ghi chép",   Icons.Filled.CameraAlt)
    object Insights : Screen("insights", "Phân tích",  Icons.Filled.AutoAwesome)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Habits,
    Screen.Camera,
    Screen.Insights
)
