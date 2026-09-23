package com.healthyai.app.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.presentation.screen.auth.LoginScreen
import com.healthyai.app.presentation.screen.camera.CameraScreen
import com.healthyai.app.presentation.screen.habits.HabitsScreen
import com.healthyai.app.presentation.screen.habits.KEY_LOG_CATEGORY
import com.healthyai.app.presentation.screen.habits.addedit.AddEditHabitScreen
import com.healthyai.app.presentation.screen.home.HomeScreen
import com.healthyai.app.presentation.screen.insights.InsightsScreen

/**
 * Điểm vào của UI.
 *
 * Trước hết là "cổng xác thực" ([SessionState]): app hiện màn đăng nhập nếu người dùng
 * chưa đăng nhập và chưa chọn chế độ khách; ngược lại mới dựng [MainNavigation] (bottom
 * nav + NavHost của 4 màn hình chính).
 */
@Composable
fun AppNavigation(viewModel: AuthGateViewModel = hiltViewModel()) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()

    when (sessionState) {
        SessionState.Loading -> AuthLoadingScreen()

        SessionState.SignedOut -> LoginScreen(
            onContinueAsGuest = viewModel::continueAsGuest
        )

        SessionState.Guest, SessionState.SignedIn -> MainNavigation(onLogout = viewModel::signOut)
    }
}

/**
 * Mở tab Thói quen và yêu cầu `HabitsViewModel` mở sẵn hộp thoại ghi nhận cho [category].
 *
 * Dùng chung cho hai lối vào:
 * - nút hành động trong thẻ gợi ý AI ở màn Phân tích (mục 3 #14);
 * - nút "Thêm vào nhật ký" ở thẻ kết quả nhận diện món ăn của màn Ghi chép — danh mục DINH DƯỠNG
 *   (phụ lục C #22).
 *
 * Điều hướng dùng đúng tuỳ chọn như bottom bar để không phá vỡ trạng thái tab, sau đó gửi danh mục
 * qua `SavedStateHandle` của destination Thói quen.
 */
private fun NavHostController.openHabitLogFor(category: HabitCategory) {
    navigate(Screen.Habits.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    currentBackStackEntry
        ?.savedStateHandle
        ?.set(KEY_LOG_CATEGORY, category.name)
}

/** Hiển thị trong lúc chờ FirebaseAuth đọc lại phiên đã lưu (thường rất ngắn). */
@Composable
private fun AuthLoadingScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = Color(0xFF2E7D32))
            Text(
                text = "HealthyAI",
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun MainNavigation(onLogout: () -> Unit) {
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
            composable(Screen.Home.route)     { HomeScreen(onLogout = onLogout) }
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
            composable(Screen.Camera.route)   {
                CameraScreen(
                    // Phụ lục C #22: kết quả camera giờ ghi được vào `habit_logs` thông qua thói quen
                    // dinh dưỡng đang hoạt động (cùng luồng với nút hành động của màn Phân tích).
                    onAddToJournal = { navController.openHabitLogFor(HabitCategory.NUTRITION) }
                )
            }
            composable(Screen.Insights.route) {
                InsightsScreen(
                    onOpenHabitLog = { category -> navController.openHabitLogFor(category) }
                )
            }

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
