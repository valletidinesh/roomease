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

    // Main app
    object Dashboard    : Screen("dashboard")
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
    BottomNavItem(Screen.Dashboard,   "Home",      Icons.Filled.Home),
    BottomNavItem(Screen.Cooking,     "Cooking",   Icons.Filled.Restaurant),
    BottomNavItem(Screen.Trash,       "Trash",     Icons.Filled.Delete),
    BottomNavItem(Screen.Washroom,    "Washroom",  Icons.Filled.CleanHands),
    BottomNavItem(Screen.Water,       "Water",     Icons.Filled.WaterDrop),
    BottomNavItem(Screen.Consumables, "Goods",     Icons.Filled.Egg),
    BottomNavItem(Screen.BuyList,     "Buy List",  Icons.Filled.ShoppingCart),
)
