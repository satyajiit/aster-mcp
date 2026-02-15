package com.aster.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aster.data.local.SettingsDataStore
import com.aster.ui.screens.home.HomeScreen
import com.aster.ui.screens.ipc.IpcDashboardScreen
import com.aster.ui.screens.logs.LogScreen
import com.aster.ui.screens.mcp.McpDashboardScreen
import com.aster.ui.screens.onboarding.OnboardingScreen
import com.aster.ui.screens.permissions.PermissionAlertScreen
import com.aster.ui.screens.permissions.PermissionsScreen
import com.aster.ui.screens.remote.RemoteConnectScreen
import com.aster.ui.screens.remote.RemoteDashboardScreen
import com.aster.ui.screens.settings.SettingsScreen
import com.aster.ui.theme.AsterTheme
import com.aster.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by settingsDataStore.themeMode.collectAsStateWithLifecycle(initialValue = "system")

            // Use nullable Boolean: null = still loading from DataStore
            val onboardingComplete by remember {
                settingsDataStore.onboardingComplete.map<Boolean, Boolean?> { it }
            }.collectAsStateWithLifecycle(initialValue = null)

            val isDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            AsterTheme(darkTheme = isDarkTheme) {
                val colors = AsterTheme.colors

                when (onboardingComplete) {
                    null -> {
                        // Splash gate: show bg-only screen while DataStore loads
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colors.bg)
                        )
                    }

                    else -> {
                        val navController = rememberNavController()
                        val context = LocalContext.current

                        // Check permissions on every resume
                        var permissionsOk by remember { mutableStateOf(true) }

                        val lifecycleOwner = LocalLifecycleOwner.current
                        DisposableEffect(lifecycleOwner) {
                            val observer = LifecycleEventObserver { _, event ->
                                if (event == Lifecycle.Event.ON_RESUME) {
                                    permissionsOk =
                                        PermissionUtils.checkAllPermissions(context).allGranted
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                        }

                        // Determine start destination
                        val startDestination = if (onboardingComplete == true) {
                            Screen.Home.route
                        } else {
                            Screen.Onboarding.route
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colors.bg)
                        ) {
                            AsterNavHost(
                                navController = navController,
                                startDestination = startDestination,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // If onboarding is complete but permissions are missing, redirect
                        LaunchedEffect(onboardingComplete, permissionsOk) {
                            if (onboardingComplete == true && !permissionsOk) {
                                navController.navigate(Screen.PermissionAlert.route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object IpcDashboard : Screen("ipc_dashboard")
    object McpDashboard : Screen("mcp_dashboard")
    object RemoteConnect : Screen("remote_connect")
    object RemoteDashboard : Screen("remote_dashboard")
    object Settings : Screen("settings")
    object PermissionAlert : Screen("permission_alert")
    object Permissions : Screen("permissions")
    object Logs : Screen("logs")
}

@Composable
fun AsterNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(
            route = Screen.Onboarding.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onNavigateToPermissions = {
                    navController.navigate(Screen.Permissions.route)
                }
            )
        }

        composable(
            route = Screen.Home.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(300)) +
                        fadeIn(animationSpec = tween(300))
            }
        ) {
            HomeScreen(
                onNavigateToIpc = { navController.navigate(Screen.IpcDashboard.route) },
                onNavigateToMcp = { navController.navigate(Screen.McpDashboard.route) },
                onNavigateToRemote = { navController.navigate(Screen.RemoteConnect.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) },
                onNavigateToIpcDashboard = { navController.navigate(Screen.IpcDashboard.route) },
                onNavigateToMcpDashboard = { navController.navigate(Screen.McpDashboard.route) },
                onNavigateToRemoteDashboard = { navController.navigate(Screen.RemoteDashboard.route) },
                onNavigateToLogs = { navController.navigate(Screen.Logs.route) }
            )
        }

        composable(
            route = Screen.IpcDashboard.route,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) +
                        fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            }
        ) {
            IpcDashboardScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) },
                onNavigateToLogs = { navController.navigate(Screen.Logs.route) }
            )
        }

        composable(
            route = Screen.McpDashboard.route,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) +
                        fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            }
        ) {
            McpDashboardScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) },
                onNavigateToLogs = { navController.navigate(Screen.Logs.route) }
            )
        }

        composable(
            route = Screen.RemoteConnect.route,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) +
                        fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            }
        ) {
            RemoteConnectScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) },
                onNavigateToDashboard = {
                    navController.navigate(Screen.RemoteDashboard.route) {
                        popUpTo(Screen.RemoteConnect.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.RemoteDashboard.route,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) +
                        fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            }
        ) {
            RemoteDashboardScreen(
                onNavigateBack = { navController.popBackStack() },
                onDisconnected = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.RemoteDashboard.route) { inclusive = true }
                    }
                },
                onNavigateToLogs = { navController.navigate(Screen.Logs.route) }
            )
        }

        composable(
            route = Screen.Settings.route,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) +
                        fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            }
        ) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Logs.route,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) +
                        fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            }
        ) {
            LogScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PermissionAlert.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            PermissionAlertScreen(
                onNavigateToPermissions = {
                    navController.navigate(Screen.Permissions.route) {
                        popUpTo(Screen.PermissionAlert.route) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.PermissionAlert.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Permissions.route,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) +
                        fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            }
        ) {
            PermissionsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
