package com.roomease.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.sp
import com.roomease.app.ui.navigation.Screen
import com.roomease.app.ui.theme.*
import com.roomease.app.ui.viewmodel.RoomViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    roomViewModel: RoomViewModel,
    onNavigateTo: (Screen) -> Unit
) {
    val me by roomViewModel.currentUser.collectAsState()
    val hasNoRoom by roomViewModel.hasNoRoom.collectAsState()
    val isLoading by roomViewModel.isLoading.collectAsState()
    
    if (isLoading) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    if (hasNoRoom) {
        OnboardingHome(onNavigateTo)
    } else {
        RoomHome(roomViewModel, onNavigateTo)
    }
}

@Composable
private fun OnboardingHome(onNavigateTo: (Screen) -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏠", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text("Welcome to RoomEase", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Organize your room, simplified.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(48.dp))
        
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomHome(roomViewModel: RoomViewModel, onNavigateTo: (Screen) -> Unit) {
    val me by roomViewModel.currentUser.collectAsState()
    val users by roomViewModel.users.collectAsState()
    val rotationStates by roomViewModel.rotationStates.collectAsState()
    val buyList by roomViewModel.buyList.collectAsState()
    val room by roomViewModel.room.collectAsState()
    
    // Helper to get name of person next in rotation
    fun getNextName(groupKey: String): String {
        val state = rotationStates[groupKey] ?: return "Not started"
        val order = room?.masterOrder ?: return "Unknown"
        if (order.isEmpty()) return "No members"
        val nextUid = order[state.cycleIndex % order.size]
        val user = users.find { it.uid == nextUid }
        return if (nextUid == me?.uid) "You" else user?.name ?: "Roommate"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(room?.name ?: "RoomEase", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Primary)
                        Text("Overview", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // "Next Up" Section
            Text("Up Next", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            StatusCard(
                title = "Cooking",
                subtitle = "Tonight's Chef: ${getNextName("COOKING")}",
                icon = Icons.Filled.Restaurant,
                color = CookingColor,
                onClick = { onNavigateTo(Screen.Cooking) }
            )
            
            StatusCard(
                title = "Trash Duty",
                subtitle = "Next: ${getNextName("TRASH")}",
                icon = Icons.Filled.Delete,
                color = TrashColor,
                onClick = { onNavigateTo(Screen.Trash) }
            )
            
            StatusCard(
                title = "Washroom",
                subtitle = "Cleaning Group 1", // Washroom uses a different group structure, keeping simple for now
                icon = Icons.Filled.CleanHands,
                color = WashroomColor,
                onClick = { onNavigateTo(Screen.Washroom) }
            )

            Spacer(Modifier.height(8.dp))
            
            // "Buy List" Preview
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Buy List", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onNavigateTo(Screen.BuyList) }) { Text("View All") }
            }
            
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (buyList.isEmpty()) {
                    Text("No items needed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    buyList.take(3).forEach { item ->
                        BuyListPreviewRow(item.itemName, item.status == com.roomease.app.data.model.BuyStatus.BOUGHT)
                    }
                }
            }
            
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StatusCard(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(color.copy(0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun BuyListPreviewRow(item: String, isBought: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (isBought) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, 
            null, 
            tint = if (isBought) BuyListColor else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(item, style = MaterialTheme.typography.bodyMedium, color = if (isBought) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
    }
}
