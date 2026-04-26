package com.roomease.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import com.roomease.app.ui.navigation.Screen
import com.roomease.app.ui.navigation.bottomNavItems
import com.roomease.app.ui.screens.*
import com.roomease.app.ui.theme.RoomEaseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoomEaseTheme {
                RoomEaseMainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomEaseMainApp() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.screen.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = androidx.compose.ui.unit.Dp(0f),
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                            selected = currentRoute == item.screen.route,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) },
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) { popUpTo(Screen.Splash.route) { inclusive = true } } },
                    onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Splash.route) { inclusive = true } } },
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToCreateRoom = { navController.navigate(Screen.CreateRoom.route) },
                    onNavigateToJoinRoom = { navController.navigate(Screen.JoinRoom.route) },
                    onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                )
            }
            composable(Screen.CreateRoom.route) {
                CreateRoomScreen(
                    onRoomCreated = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                )
            }
            composable(Screen.JoinRoom.route) {
                JoinRoomScreen(
                    onRoomJoined = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateTo = { navController.navigate(it.route) }
                )
            }
            composable(Screen.Cooking.route) {
                CookingScreen(
                    onNavigateToCalendar = { navController.navigate(Screen.CookingCalendar.route) }
                )
            }
            composable(Screen.CookingCalendar.route) {
                CookingCalendarScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Trash.route) {
                TrashScreen()
            }
            composable(Screen.Washroom.route) {
                WashroomScreen()
            }
            composable(Screen.Water.route) {
                WaterScreen()
            }
            composable(Screen.Consumables.route) {
                ConsumablesScreen()
            }
            composable(Screen.BuyList.route) {
                BuyListScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
