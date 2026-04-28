package com.roomease.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.roomease.app.SupabaseClient
import io.github.jan.supabase.auth.auth
import com.roomease.app.data.model.GroupRotationState
import com.roomease.app.data.model.User
import com.roomease.app.data.model.isEligible
import com.roomease.app.data.repository.CookingRepository
import com.roomease.app.data.repository.RoomRepository
import com.roomease.app.ui.theme.CookingColor
import com.roomease.app.ui.theme.Primary
import kotlinx.coroutines.launch
import com.roomease.app.ui.viewmodel.RoomViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingScreen(roomViewModel: RoomViewModel, onNavigateToCalendar: () -> Unit) {
    val me by roomViewModel.currentUser.collectAsState()
    val users by roomViewModel.users.collectAsState()
    val room by roomViewModel.room.collectAsState()
    val rotationStates by roomViewModel.rotationStates.collectAsState()
    val masterOrder = room?.masterOrder ?: emptyList()

    val cookingRepo = remember { com.roomease.app.data.repository.CookingRepository() }
    val groupKey = remember(users, masterOrder) { cookingRepo.buildGroupKey(users, masterOrder) }
    val cookingState = rotationStates[groupKey]

    val assignedUid = if (masterOrder.isNotEmpty()) {
        if (cookingState != null) {
            com.roomease.app.domain.RotationEngine.getAssigned(cookingState)
        } else {
            // If no state yet, assume the first person in the group key is next
            groupKey.split(",").firstOrNull()
        }
    } else null
    
    val assignedUser = users.find { it.uid == assignedUid }
    val assignedName = if (assignedUid == me?.uid) "You" else assignedUser?.name ?: "—"

    var isLoading by remember { mutableStateOf(false) }
    var showOverridePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🍳 Cooking", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(Icons.Filled.CalendarMonth, "Calendar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            // Big assignment card
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Accent top bar
                    Box(Modifier.size(48.dp).clip(CircleShape).background(CookingColor.copy(0.15f)), contentAlignment = Alignment.Center) {
                        Text("🍳", style = MaterialTheme.typography.headlineMedium)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Today's Cook", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        assignedName,
                        style = MaterialTheme.typography.displayMedium,
                        color = CookingColor,
                        fontWeight = FontWeight.ExtraBold,
                    )

                }
            }

            Spacer(Modifier.weight(1f))

            // Mark done (self)

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            cookingRepo.markDone(room?.id ?: "", groupKey, me?.uid ?: "")
                            roomViewModel.showMessage("Great job! Rotation updated. 🎉")
                            roomViewModel.refresh()
                        } catch (e: Exception) {
                            roomViewModel.showMessage(e.message ?: "Failed to update")
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CookingColor, contentColor = MaterialTheme.colorScheme.onPrimary),
                enabled = assignedUid == me?.uid && !isLoading,
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else { Icon(Icons.Filled.Check, null); Spacer(Modifier.width(8.dp)); Text("I'm Done Cooking! 🥗", fontWeight = FontWeight.Bold) }
            }

            // Override — someone else cooked
            OutlinedButton(
                onClick = { showOverridePicker = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Filled.SwapHoriz, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Someone else cooked")
            }
        }
        if (showOverridePicker) {
            MemberPicker(
                title = "Who cooked?",
                members = users.filter { it.presence == "PRESENT" },
                onDismiss = { showOverridePicker = false },
                onSelected = { user ->
                    showOverridePicker = false
                    scope.launch {
                        isLoading = true
                        try {
                            cookingRepo.markDone(room?.id ?: "", groupKey, user.uid)
                            roomViewModel.showMessage("${user.name} cooked! Rotation updated. 🥗")
                            roomViewModel.refresh()
                        } catch (e: Exception) {
                            roomViewModel.showMessage(e.message ?: "Failed to update")
                        } finally {
                            isLoading = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun MemberPicker(title: String, members: List<com.roomease.app.data.model.User>, onDismiss: () -> Unit, onSelected: (com.roomease.app.data.model.User) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(members) { user ->
                    ListItem(
                        headlineContent = { Text(user.name) },
                        modifier = Modifier.clickable { onSelected(user) },
                        leadingContent = {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(CookingColor.copy(0.1f)), contentAlignment = Alignment.Center) {
                                Text(user.name.take(1).uppercase())
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingCalendarScreen(
    roomViewModel: RoomViewModel,
    onBack: () -> Unit
) {
    val room by roomViewModel.room.collectAsState()
    val me by roomViewModel.currentUser.collectAsState()
    val cookingRepo = remember { com.roomease.app.data.repository.CookingRepository() }
    
    val users by roomViewModel.users.collectAsState()
    
    // We listen to cooking history for the user's washroom group
    val history by produceState<List<com.roomease.app.data.model.CookingHistory>>(initialValue = emptyList(), key1 = room, key2 = me) {
        if (room != null && me != null) {
            cookingRepo.listenToHistory(room!!.id, me!!.washroomGroup.toString()).collect {
                value = it
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📅 Cooking History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            if (history.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No cooking history yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(history) { entry ->
                        val cookedByName = users.find { it.uid == entry.actualUserId }?.name ?: "Unknown User"
                        val dateStr = entry.cookedAt?.substringBefore("T") ?: "Unknown Date"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(40.dp).clip(CircleShape).background(com.roomease.app.ui.theme.CookingColor.copy(0.15f)), contentAlignment = Alignment.Center) {
                                    Text("🍳")
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text("Cooked by: $cookedByName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
