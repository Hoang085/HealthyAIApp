package com.healthyai.app.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
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
import com.healthyai.app.R
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.presentation.screen.auth.LoginScreen
import com.healthyai.app.presentation.screen.camera.CameraScreen
import com.healthyai.app.presentation.screen.habits.HabitsScreen
import com.healthyai.app.presentation.screen.habits.KEY_LOG_CATEGORY
import com.healthyai.app.presentation.screen.habits.addedit.AddEditHabitScreen
import com.healthyai.app.presentation.screen.heartrate.HeartRateScreen
import com.healthyai.app.presentation.screen.home.HomeScreen
import com.healthyai.app.presentation.screen.insights.InsightsScreen
import com.healthyai.app.presentation.screen.privacy.PrivacyPolicyScreen

/** Điểm vào của UI: hiện màn đăng nhập nếu chưa đăng nhập/chưa chọn khách, ngược lại dựng MainNavigation. */
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

/** Mở tab Thói quen và yêu cầu HabitsViewModel mở sẵn hộp thoại ghi nhận cho category. */
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
                text = stringResource(R.string.app_name),
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
                            icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleRes)) },
                            label = { Text(stringResource(screen.titleRes)) },
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
            composable(Screen.Home.route)     {
                HomeScreen(
                    onLogout = onLogout,
                    // Ô "Tim" trên thẻ "Tóm tắt sức khỏe" mở màn đo nhịp tim bằng camera + đèn flash
                    onMeasureHeartRate = { navController.navigate(Screen.MeasureHeartRate.route) }
                )
            }
            composable(Screen.Habits.route) { backStackEntry ->
                // Đọc yêu cầu ghi nhận ngay tại đây từ SavedStateHandle; để ViewModel tự nghe thì giá trị bị mất khi entry dựng lại.
                val pendingLogCategory by backStackEntry.savedStateHandle
                    .getStateFlow<String?>(KEY_LOG_CATEGORY, null)
                    .collectAsStateWithLifecycle()

                HabitsScreen(
                    onNavigateToAddHabit = { 
                        navController.navigate(Screen.AddEditHabit.createRoute()) 
                    },
                    onNavigateToEditHabit = { habitId ->
                        navController.navigate(Screen.AddEditHabit.createRoute(habitId))
                    },
                    pendingLogCategory = pendingLogCategory,
                    onPendingLogCategoryConsumed = {
                        backStackEntry.savedStateHandle.remove<String>(KEY_LOG_CATEGORY)
                    }
                ) 
            }
            composable(Screen.Camera.route)   {
                CameraScreen(
                    // Kết quả camera ghi vào habit_logs qua thói quen dinh dưỡng đang hoạt động (cùng luồng với màn Phân tích).
                    onAddToJournal = { navController.openHabitLogFor(HabitCategory.NUTRITION) }
                )
            }
            composable(Screen.Insights.route) {
                InsightsScreen(
                    onOpenHabitLog = { category -> navController.openHabitLogFor(category) },
                    // Lối vào Chính sách bảo mật của phần AI nay ở tab Phân tích, nơi người dùng đọc về việc ảnh gửi tới Gemini.
                    onOpenPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) }
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

            composable(Screen.MeasureHeartRate.route) {
                HeartRateScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.PrivacyPolicy.route) {
                PrivacyPolicyScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
