package com.roomease.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    // Auth / Onboarding
    object Splash       : Screen("splash")
    object Login        : Screen("login")
    object CreateRoom   : Screen("create_room")
    object JoinRoom     : Screen("join_room")

    // Main app tabs
    object Home         : Screen("home")
    object Dashboard    : Screen("dashboard")
    object Account      : Screen("account")

    // Chore screens
    object Cooking      : Screen("cooking")
    object CookingCalendar : Screen("cooking_calendar")
    object Trash        : Screen("trash")
    object Washroom     : Screen("washroom")
    object Water        : Screen("water")
    object Consumables  : Screen("consumables")
    object BuyList      : Screen("buy_list")
    object Settings     : Screen("settings")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,      "Home",      Icons.Filled.Home),
    BottomNavItem(Screen.Dashboard, "Dashboard", Icons.Filled.GridView),
    BottomNavItem(Screen.Account,   "Account",   Icons.Filled.Person),
)
