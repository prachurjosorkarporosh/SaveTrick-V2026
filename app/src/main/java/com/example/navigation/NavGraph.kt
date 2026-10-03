package com.example.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.R
import com.example.data.repository.SaveTrickRepository
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.admin.AdminLoginScreen
import com.example.ui.screens.downloader.DownloaderScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.startup.OnboardingScreen
import com.example.ui.screens.startup.StartupScreen
import com.example.ui.theme.ElectricBlue

object Destinations {
    const val STARTUP = "startup"
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val ADMIN_LOGIN = "admin_login"
    const val ADMIN_DASHBOARD = "admin_dashboard"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    repository: SaveTrickRepository,
    onThemeChanged: (String) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.STARTUP,
        enterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally(animationSpec = tween(300)) { it / 3 } },
        exitTransition = { fadeOut(animationSpec = tween(250)) + slideOutHorizontally(animationSpec = tween(250)) { -it / 3 } },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally(animationSpec = tween(300)) { -it / 3 } },
        popExitTransition = { fadeOut(animationSpec = tween(250)) + slideOutHorizontally(animationSpec = tween(250)) { it / 3 } }
    ) {
        composable(Destinations.STARTUP) {
            StartupScreen(
                repository = repository,
                onNavigateToHome = {
                    navController.navigate(Destinations.MAIN) {
                        popUpTo(Destinations.STARTUP) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Destinations.ONBOARDING) {
                        popUpTo(Destinations.STARTUP) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.ONBOARDING) {
            OnboardingScreen(
                repository = repository,
                onComplete = {
                    navController.navigate(Destinations.MAIN) {
                        popUpTo(Destinations.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.MAIN) {
            MainAppScaffold(
                repository = repository,
                onNavigateToAdminLogin = {
                    navController.navigate(Destinations.ADMIN_LOGIN)
                },
                onThemeChanged = onThemeChanged
            )
        }

        composable(Destinations.ADMIN_LOGIN) {
            AdminLoginScreen(
                repository = repository,
                onLoginSuccess = {
                    navController.navigate(Destinations.ADMIN_DASHBOARD) {
                        popUpTo(Destinations.ADMIN_LOGIN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                repository = repository,
                onLogout = {
                    navController.navigate(Destinations.MAIN) {
                        popUpTo(Destinations.ADMIN_DASHBOARD) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun MainAppScaffold(
    repository: SaveTrickRepository,
    onNavigateToAdminLogin: () -> Unit,
    onThemeChanged: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val isPro by repository.isPro.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Download, contentDescription = stringResource(R.string.nav_downloader)) },
                    label = { Text(stringResource(R.string.nav_downloader)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = ElectricBlue
                    ),
                    modifier = Modifier.testTag("nav_item_downloader")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = stringResource(R.string.nav_library)) },
                    label = { Text(stringResource(R.string.nav_library)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = ElectricBlue
                    ),
                    modifier = Modifier.testTag("nav_item_library")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings)) },
                    label = { Text(stringResource(R.string.nav_settings)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = ElectricBlue
                    ),
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(animationSpec = tween(280)) { it / 4 } + fadeIn(animationSpec = tween(280)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(240)) { -it / 4 } + fadeOut(animationSpec = tween(240)))
                } else {
                    (slideInHorizontally(animationSpec = tween(280)) { -it / 4 } + fadeIn(animationSpec = tween(280)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(240)) { it / 4 } + fadeOut(animationSpec = tween(240)))
                }
            },
            label = "tab_transition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { targetTab ->
            when (targetTab) {
                0 -> DownloaderScreen(
                    repository = repository,
                    isPro = isPro,
                    onNavigateToSettings = { selectedTab = 2 },
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier.fillMaxSize()
                )
                1 -> LibraryScreen(
                    repository = repository,
                    modifier = Modifier.fillMaxSize()
                )
                2 -> SettingsScreen(
                    repository = repository,
                    onNavigateToAdminLogin = onNavigateToAdminLogin,
                    onThemeChanged = onThemeChanged,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
