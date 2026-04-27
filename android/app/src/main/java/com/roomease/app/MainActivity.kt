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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import com.roomease.app.ui.navigation.Screen
import com.roomease.app.ui.navigation.bottomNavItems
import com.roomease.app.ui.screens.*
import com.roomease.app.ui.theme.RoomEaseTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roomease.app.ui.viewmodel.RoomViewModel
import kotlinx.coroutines.flow.collectLatest
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus

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
    val roomViewModel: RoomViewModel = viewModel()
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    LaunchedEffect(Unit) {
        com.roomease.app.SupabaseClient.client.auth.sessionStatus.collectLatest { status ->
            if (status is SessionStatus.NotAuthenticated) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0)
                }
            }
        }
    }

    val hasNoRoom by roomViewModel.hasNoRoom.collectAsState()

    val currentBottomNavItems = if (hasNoRoom) {
        bottomNavItems.filter { it.screen == Screen.Home || it.screen == Screen.Account }
    } else {
        bottomNavItems
    }

    val showBottomBar = currentRoute in bottomNavItems.map { it.screen.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    currentBottomNavItems.forEach { item ->
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
                    onNavigateToDashboard = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Splash.route) { inclusive = true } } },
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToCreateRoom = { navController.navigate(Screen.CreateRoom.route) },
                    onNavigateToJoinRoom = { navController.navigate(Screen.JoinRoom.route) },
                    onNavigateToDashboard = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                )
            }
            composable(Screen.CreateRoom.route) {
                CreateRoomScreen(
                    onRoomCreated = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                )
            }
            composable(Screen.JoinRoom.route) {
                JoinRoomScreen(
                    onRoomJoined = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    roomViewModel = roomViewModel,
                    onNavigateTo = { navController.navigate(it.route) }
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    roomViewModel = roomViewModel,
                    onNavigateTo = { navController.navigate(it.route) }
                )
            }
            composable(Screen.Account.route) {
                AccountScreen(
                    roomViewModel = roomViewModel
                )
            }
            composable(Screen.Cooking.route) {
                CookingScreen(
                    roomViewModel = roomViewModel,
                    onNavigateToCalendar = { navController.navigate(Screen.CookingCalendar.route) }
                )
            }
            composable(Screen.CookingCalendar.route) {
                CookingCalendarScreen(
                    roomViewModel = roomViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Trash.route) {
                TrashScreen(roomViewModel)
            }
            composable(Screen.Washroom.route) {
                WashroomScreen(roomViewModel)
            }
            composable(Screen.Water.route) {
                WaterScreen(roomViewModel)
            }
            composable(Screen.Consumables.route) {
                ConsumablesScreen(roomViewModel)
            }
            composable(Screen.BuyList.route) {
                BuyListScreen(roomViewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    roomViewModel = roomViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
