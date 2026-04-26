package com.roomease.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roomease.app.ui.navigation.Screen
import com.roomease.app.ui.theme.*
import com.roomease.app.ui.viewmodel.RoomViewModel
import com.roomease.app.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// DashboardScreen — home screen showing quick status of all 6 modules
// Real-time listeners will be wired via ViewModel in Phase 4; for now it shows
// the structure so compilation succeeds from day 1.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DashboardScreen(
    roomViewModel: RoomViewModel,
    onNavigateTo: (Screen) -> Unit
) {
    val me by roomViewModel.currentUser.collectAsState()
    val hasNoRoom by roomViewModel.hasNoRoom.collectAsState()
    val isLoading by roomViewModel.isLoading.collectAsState()
    val scope = rememberCoroutineScope()

    if (isLoading) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    if (hasNoRoom) {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🏠", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Text("Welcome to RoomEase", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("You don't belong to any room yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            
            Button(
                onClick = { onNavigateTo(Screen.CreateRoom) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Create a New Room", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { onNavigateTo(Screen.JoinRoom) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.MeetingRoom, null)
                Spacer(Modifier.width(8.dp))
                Text("Join an Existing Room", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(48.dp))
            TextButton(
                onClick = {
                    scope.launch { SupabaseClient.client.auth.signOut() }
                }
            ) {
                Text("Sign Out", color = MaterialTheme.colorScheme.error)
            }
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        DashboardHeader(
            userName = me?.name,
            onNavigateTo = onNavigateTo
        )

        Spacer(Modifier.height(8.dp))

        // Task cards
        Column(
            Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TaskCard(
                emoji = "🍳",
                title = "Cooking",
                subtitle = "Today's cook",
                accentColor = CookingColor,
                onClick = { onNavigateTo(Screen.Cooking) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TaskCard(
                    emoji = "🗑️",
                    title = "Trash",
                    subtitle = "WET & DRY",
                    accentColor = TrashColor,
                    onClick = { onNavigateTo(Screen.Trash) },
                    modifier = Modifier.weight(1f),
                )
                TaskCard(
                    emoji = "🚿",
                    title = "Washroom",
                    subtitle = "Cleaning turns",
                    accentColor = WashroomColor,
                    onClick = { onNavigateTo(Screen.Washroom) },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TaskCard(
                    emoji = "💧",
                    title = "Water",
                    subtitle = "Next pair",
                    accentColor = WaterColor,
                    onClick = { onNavigateTo(Screen.Water) },
                    modifier = Modifier.weight(1f),
                )
                TaskCard(
                    emoji = "🥚",
                    title = "Consumables",
                    subtitle = "Split tracking",
                    accentColor = ConsumeColor,
                    onClick = { onNavigateTo(Screen.Consumables) },
                    modifier = Modifier.weight(1f),
                )
            }
            TaskCard(
                emoji = "🛒",
                title = "Buy List",
                subtitle = "Shared shopping",
                accentColor = BuyListColor,
                onClick = { onNavigateTo(Screen.BuyList) },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DashboardHeader(userName: String?, onNavigateTo: (Screen) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "RoomEase",
                style = MaterialTheme.typography.headlineMedium,
                color = Primary,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "Good evening, ${userName ?: "Roommate"} 🌙",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { onNavigateTo(Screen.Settings) }) {
            Icon(Icons.Filled.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TaskCard(
    emoji: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
    ) {
        // Accent top line
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(accentColor)
                .align(Alignment.TopCenter)
        )

        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emoji, style = MaterialTheme.typography.titleLarge)
                }
                badge?.let {
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.2f),
                    ) {
                        Text(it, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = accentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
