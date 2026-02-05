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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aster.data.local.SettingsDataStore
import com.aster.data.model.ConnectionState
import com.aster.data.websocket.AsterWebSocketClient
import com.aster.service.AsterService
import com.aster.ui.screens.connect.ConnectScreen
import com.aster.ui.screens.permissions.PermissionsScreen
import com.aster.ui.screens.status.StatusScreen
import com.aster.ui.theme.AsterTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var webSocketClient: AsterWebSocketClient

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AsterTheme {
                val navController = rememberNavController()
                val connectionState by webSocketClient.connectionState.collectAsState()
                val colors = com.aster.ui.theme.AsterTheme.colors

                // Edge-to-edge: let each screen handle its own insets
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.terminalBg)
                ) {
                    AsterNavHost(
                        navController = navController,
                        webSocketClient = webSocketClient,
                        settingsDataStore = settingsDataStore,
                        connectionState = connectionState,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Auto-navigate based on connection state
                LaunchedEffect(connectionState) {
                    when (connectionState) {
                        ConnectionState.APPROVED -> {
                            navController.navigate(Screen.Status.route) {
                                popUpTo(Screen.Connect.route) { inclusive = true }
                            }
                        }
                        ConnectionState.DISCONNECTED -> {
                            if (navController.currentDestination?.route == Screen.Status.route) {
                                navController.navigate(Screen.Connect.route) {
                                    popUpTo(Screen.Status.route) { inclusive = true }
                                }
                            }
                        }
                        else -> { /* handled by screens */ }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't shutdown WebSocket here - let the service manage it
    }
}

sealed class Screen(val route: String) {
    object Connect : Screen("connect")
    object Status : Screen("status")
    object Permissions : Screen("permissions")
}

@Composable
fun AsterNavHost(
    navController: NavHostController,
    webSocketClient: AsterWebSocketClient,
    settingsDataStore: SettingsDataStore,
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Connect.route,
        modifier = modifier
    ) {
        composable(
            route = Screen.Connect.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            ConnectScreen(
                webSocketClient = webSocketClient,
                settingsDataStore = settingsDataStore,
                connectionState = connectionState,
                onNavigateToPermissions = {
                    navController.navigate(Screen.Permissions.route)
                }
            )
        }

        composable(
            route = Screen.Status.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            val context = LocalContext.current
            StatusScreen(
                webSocketClient = webSocketClient,
                connectionState = connectionState,
                onDisconnect = {
                    // Stop the foreground service (which handles WebSocket disconnection)
                    AsterService.stopService(context)
                    navController.navigate(Screen.Connect.route) {
                        popUpTo(Screen.Status.route) { inclusive = true }
                    }
                },
                onNavigateToPermissions = {
                    navController.navigate(Screen.Permissions.route)
                }
            )
        }

        composable(
            route = Screen.Permissions.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            PermissionsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
