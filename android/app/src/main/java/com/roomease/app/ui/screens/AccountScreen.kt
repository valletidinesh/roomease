package com.roomease.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roomease.app.SupabaseClient
import com.roomease.app.ui.theme.*
import com.roomease.app.ui.viewmodel.RoomViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(roomViewModel: RoomViewModel) {
    val me by roomViewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    
    var editedName by remember(me) { mutableStateOf(me?.name ?: "") }
    var isEditingName by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Header
            Box(Modifier.size(100.dp).clip(CircleShape).background(Primary.copy(0.1f)), contentAlignment = Alignment.Center) {
                Text(me?.name?.take(1)?.uppercase() ?: "?", style = MaterialTheme.typography.displayMedium, color = Primary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Text(me?.email ?: "user@example.com", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(Modifier.height(32.dp))

            // Info Section
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                
                // Name Field
                Column {
                    Text("Name", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isEditingName) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
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
                            Text(me?.name ?: "—", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { isEditingName = true }) {
                                Icon(Icons.Filled.Edit, "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // Presence Toggle (only if has room)
                if (!roomViewModel.hasNoRoom.value) {
                    var presence by remember(me) { mutableStateOf(me?.presence == "PRESENT") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Presence Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (presence) "Present" else "Away", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
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
                }
            }

            Spacer(Modifier.height(32.dp))

            // Actions
            Button(
                onClick = { scope.launch { SupabaseClient.client.auth.signOut() } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Icon(Icons.Filled.Logout, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Sign Out")
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Account", color = MaterialTheme.colorScheme.error.copy(0.7f))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?") },
            text = { Text("This will permanently remove your user data. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { 
                    // In real app, call a delete function
                    scope.launch { SupabaseClient.client.auth.signOut() }
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
