package com.roomease.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.roomease.app.SupabaseClient
import com.roomease.app.data.model.BuyStatus
import com.roomease.app.ui.theme.*
import io.github.jan.supabase.auth.auth

// ─────────────────────────────────────────────────────────────────────────────
// ConsumablesScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumablesScreen() {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🥚 Consumables", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Filled.Add, "Add purchase entry") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)
        ) {
            // Tab: Open / Closed
            var tab by remember { mutableIntStateOf(0) }
            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Open", fontWeight = if (tab == 0) FontWeight.Bold else FontWeight.Normal) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Closed", fontWeight = if (tab == 1) FontWeight.Bold else FontWeight.Normal) })
            }

            LazyColumn(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                if (tab == 0) {
                    // Placeholder entry
                    item {
                        ConsumableEntryCard(
                            item = "Eggs",
                            boughtBy = "You",
                            totalQty = 30,
                            totalPrice = 300.0,
                            usedSoFar = 12,
                            isOpen = true,
                            onLogUsage = {},
                            onClose = {},
                        )
                    }
                } else {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No closed entries yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (showAddDialog) AddEntryDialog(onDismiss = { showAddDialog = false }, onAdd = { showAddDialog = false })
    }
}

@Composable
private fun ConsumableEntryCard(
    item: String,
    boughtBy: String,
    totalQty: Int,
    totalPrice: Double,
    usedSoFar: Int,
    isOpen: Boolean,
    onLogUsage: () -> Unit,
    onClose: () -> Unit,
) {
    val remaining = totalQty - usedSoFar
    val progress = usedSoFar.toFloat() / totalQty.coerceAtLeast(1)

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(8.dp), color = if (isOpen) ConsumeColor.copy(0.15f) else MaterialTheme.colorScheme.surfaceVariant) {
                Text(if (isOpen) "OPEN" else "CLOSED", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = if (isOpen) ConsumeColor else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column { Text("Bought by", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(boughtBy, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface) }
            Column(horizontalAlignment = Alignment.End) { Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("₹${"%.0f".format(totalPrice)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface) }
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = ConsumeColor, trackColor = MaterialTheme.colorScheme.surfaceVariant)
        Text("$usedSoFar / $totalQty used · $remaining remaining", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (isOpen) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onLogUsage, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.Add, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Log Usage", style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = onClose, shape = RoundedCornerShape(10.dp)) {
                    Text("Close batch", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AddEntryDialog(onDismiss: () -> Unit, onAdd: () -> Unit) {
    var item by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Purchase Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = item, onValueChange = { item = it }, label = { Text("Item (e.g. Eggs, Bread)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Total price (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
            }
        },
        confirmButton = {
            Button(onClick = onAdd, enabled = item.isNotBlank() && qty.isNotBlank() && price.isNotBlank(), shape = RoundedCornerShape(10.dp)) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// BuyListScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyListScreen() {
    var showAddDialog by remember { mutableStateOf(false) }
    // Placeholder items
    val items = remember {
        mutableStateListOf(
            "Eggs" to BuyStatus.PENDING,
            "Bread" to BuyStatus.BOUGHT,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🛒 Buy List", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BuyListColor,
                contentColor = MaterialTheme.colorScheme.background,
            ) { Icon(Icons.Filled.Add, "Add item") }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            items(items) { (name, status) ->
                BuyListItemRow(
                    name = name,
                    status = status,
                    onMarkBought = { /* markBought */ },
                    onDelete = { items.removeIf { it.first == name } },
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog) {
        var newItem by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add to buy list") },
            text = {
                OutlinedTextField(value = newItem, onValueChange = { newItem = it }, label = { Text("Item name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
            },
            confirmButton = {
                Button(onClick = { if (newItem.isNotBlank()) { items.add(newItem.trim() to BuyStatus.PENDING); showAddDialog = false } }, shape = RoundedCornerShape(10.dp)) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
private fun BuyListItemRow(name: String, status: BuyStatus, onMarkBought: () -> Unit, onDelete: () -> Unit) {
    val done = status == BuyStatus.BOUGHT
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).clickable(enabled = !done, onClick = onMarkBought).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            null,
            tint = if (done) BuyListColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (!done) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SettingsScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var presence by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            // Presence toggle
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("I'm home today", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(if (presence) "You are PRESENT — included in all rotations" else "You are AWAY — skipped in all tasks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = presence, onCheckedChange = { presence = it }, colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = Primary.copy(0.3f)))
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)
            Text("Room Info", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsRow(icon = Icons.Filled.Share, label = "Invite Code", value = "••••••")
                SettingsRow(icon = Icons.Filled.People, label = "Members", value = "—")
                SettingsRow(icon = Icons.Filled.Home, label = "Room name", value = "—")
            }

            Spacer(Modifier.weight(1f))

            OutlinedButton(
                onClick = {
                    scope.launch { SupabaseClient.client.auth.signOut() }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.Filled.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
