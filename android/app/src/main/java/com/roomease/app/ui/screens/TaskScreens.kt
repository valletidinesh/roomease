package com.roomease.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roomease.app.data.model.TrashType
import com.roomease.app.ui.theme.*
import com.roomease.app.ui.viewmodel.RoomViewModel
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// TrashScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(roomViewModel: RoomViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("WET 💧", "DRY 📦")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🗑️ Trash", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { i, label ->
                    Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(label, fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal) })
                }
            }

            val trashType = if (selectedTab == 0) TrashType.WET else TrashType.DRY
            TrashTypePanel(trashType = trashType, roomViewModel = roomViewModel)
        }
    }
}

@Composable
private fun TrashTypePanel(trashType: TrashType, roomViewModel: RoomViewModel) {
    val me by roomViewModel.currentUser.collectAsState()
    val users by roomViewModel.users.collectAsState()
    val room by roomViewModel.room.collectAsState()
    val rotationStates by roomViewModel.rotationStates.collectAsState()
    val scope = rememberCoroutineScope()
    val trashRepo = remember { com.roomease.app.data.repository.TrashRepository() }

    val groupKey = if (trashType == TrashType.WET) "TRASH_WET" else "TRASH_DRY"
    val trashState = rotationStates[groupKey]
    val masterOrder = room?.masterOrder ?: emptyList()
    
    val assignedUid = if (masterOrder.isNotEmpty() && trashState != null) {
        masterOrder[trashState.cycleIndex % masterOrder.size]
    } else null
    
    val assignedUser = users.find { it.uid == assignedUid }
    val assignedName = if (assignedUid == me?.uid) "You" else assignedUser?.name ?: "—"

    val accentColor = if (trashType == TrashType.WET) WaterColor else BuyListColor
    var isLoading by remember { mutableStateOf(false) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // Next thrower card
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (trashType == TrashType.WET) "💧" else "📦", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(12.dp))
                Text("Next to throw ${trashType.name.lowercase()} trash", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(assignedName, style = MaterialTheme.typography.displayMedium, color = accentColor, fontWeight = FontWeight.ExtraBold)
            }
        }

        // Leaderboard hint
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Complete turns per person", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Filled.Leaderboard, null, tint = accentColor)
            }
        }

        successMsg?.let {
            Text(it, color = accentColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    try {
                        trashRepo.markDone(room?.id ?: "", me?.uid ?: "", trashType)
                        successMsg = "Trash marked done! 🎉"
                        roomViewModel.refresh()
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TrashColor),
            enabled = assignedUid == me?.uid && !isLoading,
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            else {
                Icon(Icons.Filled.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Mark ${trashType.name.lowercase()} trash done", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WashroomScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WashroomScreen(roomViewModel: RoomViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🚿 Washroom", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            WashroomCard(number = 1, roomViewModel = roomViewModel)
            WashroomCard(number = 2, roomViewModel = roomViewModel)
        }
    }
}

@Composable
private fun WashroomCard(number: Int, roomViewModel: RoomViewModel) {
    val me by roomViewModel.currentUser.collectAsState()
    val users by roomViewModel.users.collectAsState()
    val room by roomViewModel.room.collectAsState()
    val washroomStates by roomViewModel.washroomStates.collectAsState()
    val scope = rememberCoroutineScope()
    val washroomRepo = remember { com.roomease.app.data.repository.WashroomRepository() }

    val state = washroomStates[number]
    val groupOrder = state?.groupOrder ?: emptyList()
    val currentGroupId = if (groupOrder.isNotEmpty() && state != null) {
        groupOrder[state.cycleIndex % groupOrder.size]
    } else ""
    
    val membersInGroup = users.filter { it.washroomGroup.toString() == currentGroupId }
    val assignedName = if (membersInGroup.isEmpty()) "—" 
        else if (membersInGroup.any { it.uid == me?.uid }) "Your Group"
        else membersInGroup.joinToString(", ") { it.name }

    var isLoading by remember { mutableStateOf(false) }

    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface)
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(WashroomColor))
        Column(Modifier.padding(top = 3.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(WashroomColor.copy(0.12f)), contentAlignment = Alignment.Center) {
                    Text("🚿", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Washroom $number", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Next cleaner: $assignedName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(8.dp), color = WashroomColor.copy(0.15f)) {
                    Text("ACTIVE", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = WashroomColor, fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = { 
                    scope.launch {
                        isLoading = true
                        try {
                            washroomRepo.markCleaned(room?.id ?: "", number)
                            roomViewModel.refresh()
                        } catch (e: Exception) {
                            // Error
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WashroomColor, contentColor = MaterialTheme.colorScheme.background),
                enabled = membersInGroup.any { it.uid == me?.uid } && !isLoading,
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), color = MaterialTheme.colorScheme.background)
                else {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Mark Cleaned", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WaterScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterScreen(roomViewModel: RoomViewModel) {
    val me by roomViewModel.currentUser.collectAsState()
    val users by roomViewModel.users.collectAsState()
    val room by roomViewModel.room.collectAsState()
    val rotationStates by roomViewModel.rotationStates.collectAsState()
    val scope = rememberCoroutineScope()
    val waterRepo = remember { com.roomease.app.data.repository.WaterRepository() }

    val waterState = rotationStates["WATER"]
    val masterOrder = room?.masterOrder ?: emptyList()
    
    val pair = if (masterOrder.size >= 2 && waterState != null) {
        val idx1 = waterState.cycleIndex % masterOrder.size
        val idx2 = (waterState.cycleIndex + 1) % masterOrder.size
        masterOrder[idx1] to masterOrder[idx2]
    } else null
    
    val user1 = users.find { it.uid == pair?.first }
    val user2 = users.find { it.uid == pair?.second }
    
    val isMyTurn = pair?.first == me?.uid || pair?.second == me?.uid

    var isLoading by remember { mutableStateOf(false) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💧 Water Cans", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("💧", style = MaterialTheme.typography.displayMedium)
                    Text("Next pair to fetch water", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        PersonBadge(name = if (pair?.first == me?.uid) "You" else user1?.name ?: "—", color = WaterColor)
                        Text("+", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        PersonBadge(name = if (pair?.second == me?.uid) "You" else user2?.name ?: "—", color = WaterColor)
                    }
                    Text("Sorted by lowest fetch count", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { 
                    scope.launch {
                        isLoading = true
                        try {
                            waterRepo.markDone(room?.id ?: "", users)
                            successMsg = "Water fetched! 💧"
                            roomViewModel.refresh()
                        } catch (e: Exception) {
                            // Error
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WaterColor, contentColor = MaterialTheme.colorScheme.onPrimary),
                enabled = isMyTurn && !isLoading,
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else {
                    Icon(Icons.Filled.WaterDrop, null)
                    Spacer(Modifier.width(8.dp))
                    Text("We Fetched Water 💧", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PersonBadge(name: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(color.copy(0.15f)), contentAlignment = Alignment.Center) {
            Text(name.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(4.dp))
        Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
