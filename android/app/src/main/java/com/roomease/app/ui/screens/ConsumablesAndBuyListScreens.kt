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
import kotlinx.coroutines.launch
import com.roomease.app.ui.viewmodel.RoomViewModel

// ─────────────────────────────────────────────────────────────────────────────
// ConsumablesScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumablesScreen(roomViewModel: RoomViewModel) {
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

            val consumables by roomViewModel.consumables.collectAsState()
            val logs by roomViewModel.usageLogs.collectAsState()
            val me by roomViewModel.currentUser.collectAsState()
            val users by roomViewModel.users.collectAsState()
            val scope = rememberCoroutineScope()
            val consumablesRepo = remember { com.roomease.app.data.repository.ConsumablesRepository() }

            LazyColumn(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                
                val filtered = consumables.filter { (it.status == "OPEN") == (tab == 0) }
                
                if (filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(if (tab == 0) "No open entries" else "No closed entries", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { entry ->
                        val entryLogs = logs[entry.id] ?: emptyList()
                        val usedSoFar = entryLogs.sumOf { it.qty }
                        val buyerUser = users.find { it.uid == entry.boughtBy }
                        
                        var showLogDialog by remember { mutableStateOf(false) }

                        ConsumableEntryCard(
                            item = entry.item,
                            boughtBy = if (entry.boughtBy == me?.uid) "You" else buyerUser?.name ?: "Roommate",
                            totalQty = entry.totalQty,
                            totalPrice = entry.totalPrice,
                            usedSoFar = usedSoFar,
                            isOpen = entry.status == "OPEN",
                            onLogUsage = { showLogDialog = true },
                            onClose = { 
                                scope.launch { consumablesRepo.closeEntry(entry.roomId, entry.id) }
                            },
                        )

                        if (showLogDialog) {
                            LogUsageDialog(
                                entry = entry,
                                usedSoFar = usedSoFar,
                                onDismiss = { showLogDialog = false },
                                onLog = { qty ->
                                    scope.launch {
                                        me?.let { consumablesRepo.logUsage(entry.roomId, entry.id, it.uid, qty) }
                                        showLogDialog = false
                                    }
                                }
                            )
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
fun BuyListScreen(roomViewModel: RoomViewModel) {
    val items by roomViewModel.buyList.collectAsState()
    val me by roomViewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    val buyRepo = remember { com.roomease.app.data.repository.BuyListRepository() }
    
    var showAddDialog by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(Modifier.height(10.dp)) }
            
            if (items.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No items needed yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(items, key = { it.id }) { item ->
                BuyListItemRow(
                    name = item.itemName,
                    status = item.status,
                    onMarkBought = { 
                        scope.launch { 
                            me?.let { buyRepo.markBought(item.roomId, item.id, it.uid) }
                        }
                    },
                    onDelete = {
                        scope.launch { buyRepo.deleteItem(item.roomId, item.id) }
                    }
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
                Button(
                    onClick = { 
                        if (newItem.isNotBlank()) { 
                            scope.launch {
                                me?.let { buyRepo.addItem(it.roomId, newItem.trim(), it.uid) }
                                showAddDialog = false 
                            }
                        } 
                    }, 
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Add") }
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

@Composable
private fun LogUsageDialog(
    entry: com.roomease.app.data.model.PurchaseEntry,
    usedSoFar: Int,
    onDismiss: () -> Unit,
    onLog: (Int) -> Unit
) {
    var qtyText by remember { mutableStateOf("") }
    val remaining = entry.totalQty - usedSoFar
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Usage: ${entry.item}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Total: ${entry.totalQty} | Remaining: $remaining", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Quantity used") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyText.toIntOrNull() ?: 0
                    if (qty > 0 && qty <= remaining) onLog(qty)
                },
                enabled = (qtyText.toIntOrNull() ?: 0) in 1..remaining,
                shape = RoundedCornerShape(10.dp)
            ) { Text("Log") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SettingsScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    roomViewModel: RoomViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val room by roomViewModel.room.collectAsState()
    val me by roomViewModel.currentUser.collectAsState()
    val users by roomViewModel.users.collectAsState()
    
    // Default to true if not loaded
    var presence by remember(me) { mutableStateOf(me?.presence == "PRESENT") }

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
                Switch(
                    checked = presence, 
                    onCheckedChange = { 
                        presence = it
                        roomViewModel.updatePresence(it)
                    }, 
                    colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = Primary.copy(0.3f))
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)
            Text("Profile", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            var editedName by remember(me) { mutableStateOf(me?.name ?: "") }
            var isEditingName by remember { mutableStateOf(false) }

            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    if (isEditingName) {
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(onClick = { 
                            if (editedName.isNotBlank()) {
                                roomViewModel.updateName(editedName.trim())
                                isEditingName = false
                            }
                        }) {
                            Icon(Icons.Filled.Check, "Save", tint = Primary)
                        }
                    } else {
                        Text("Name", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text(me?.name ?: "—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(onClick = { isEditingName = true }) {
                            Icon(Icons.Filled.Edit, "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                SettingsRow(icon = Icons.Filled.Email, label = "Email", value = me?.email ?: "—")
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)
            Text("Room Info", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsRow(icon = Icons.Filled.Share, label = "Invite Code", value = room?.inviteCode ?: "••••••")
                SettingsRow(icon = Icons.Filled.People, label = "Members", value = if (users.isEmpty()) "—" else users.joinToString(", ") { it.name })
                SettingsRow(icon = Icons.Filled.Home, label = "Room name", value = room?.name ?: "—")
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
