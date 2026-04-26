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
            TrashTypePanel(trashType = trashType)
        }
    }
}

@Composable
private fun TrashTypePanel(trashType: TrashType) {
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
                Text("—", style = MaterialTheme.typography.displayMedium, color = accentColor, fontWeight = FontWeight.ExtraBold)
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
            onClick = { successMsg = "Trash marked done! 🎉" },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TrashColor),
            enabled = !isLoading,
        ) {
            Icon(Icons.Filled.Check, null)
            Spacer(Modifier.width(8.dp))
            Text("Mark ${trashType.name.lowercase()} trash done", fontWeight = FontWeight.Bold)
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
            WashroomCard(number = 1)
            WashroomCard(number = 2)
        }
    }
}

@Composable
private fun WashroomCard(number: Int) {
    var isLoading by remember { mutableStateOf(false) }
    var markedDone by remember { mutableStateOf(false) }

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
                    Text("Next cleaner: —", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(8.dp), color = WashroomColor.copy(0.15f)) {
                    Text("ACTIVE", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = WashroomColor, fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = { markedDone = true },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WashroomColor, contentColor = MaterialTheme.colorScheme.background),
                enabled = !markedDone,
            ) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (markedDone) "Cleaned ✓" else "Mark Cleaned", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
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
    var markedDone by remember { mutableStateOf(false) }

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
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        PersonBadge(name = "—", color = WaterColor)
                        Text("+", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        PersonBadge(name = "—", color = WaterColor)
                    }
                    Text("Sorted by lowest fetch count", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { markedDone = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WaterColor, contentColor = MaterialTheme.colorScheme.background),
                enabled = !markedDone,
            ) {
                Icon(Icons.Filled.Check, null)
                Spacer(Modifier.width(8.dp))
                Text(if (markedDone) "Done! Counts updated ✓" else "Water Fetched — Mark Done", fontWeight = FontWeight.Bold)
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
