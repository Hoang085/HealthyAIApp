package com.healthyai.app.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.healthyai.app.presentation.screen.camera.CameraScreen
import com.healthyai.app.presentation.screen.habits.HabitsScreen
import com.healthyai.app.presentation.screen.habits.addedit.AddEditHabitScreen
import com.healthyai.app.presentation.screen.home.HomeScreen
import com.healthyai.app.presentation.screen.insights.InsightsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Chỉ hiện bottom bar ở các màn hình chính
            val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn() + slideInHorizontally { it } },
            exitTransition = { fadeOut() + slideOutHorizontally { -it } },
            popEnterTransition = { fadeIn() + slideInHorizontally { -it } },
            popExitTransition = { fadeOut() + slideOutHorizontally { it } }
        ) {
            composable(Screen.Home.route)     { HomeScreen() }
            composable(Screen.Habits.route)   { 
                HabitsScreen(
                    onNavigateToAddHabit = { 
                        navController.navigate(Screen.AddEditHabit.createRoute()) 
                    },
                    onNavigateToEditHabit = { habitId ->
                        navController.navigate(Screen.AddEditHabit.createRoute(habitId))
                    }
                ) 
            }
            composable(Screen.Camera.route)   { CameraScreen() }
            composable(Screen.Insights.route) { InsightsScreen() }

            composable(
                route = Screen.AddEditHabit.route,
                arguments = listOf(
                    navArgument("habitId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) {
                AddEditHabitScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
