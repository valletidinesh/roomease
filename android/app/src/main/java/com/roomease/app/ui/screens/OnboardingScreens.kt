package com.roomease.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.roomease.app.data.model.User
import com.roomease.app.data.repository.RoomRepository
import com.roomease.app.ui.theme.*
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// CreateRoomScreen — 4-step wizard
// Step 0: Room name → Step 1: Add members → Step 2: Rotation order → Step 3: Washrooms
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomScreen(onRoomCreated: () -> Unit) {
    val scope = rememberCoroutineScope()
    val repo = remember { RoomRepository() }

    var step by remember { mutableIntStateOf(0) }
    var roomName by remember { mutableStateOf("") }
    val members = remember { mutableStateListOf<MemberDraft>() }
    var masterOrder by remember { mutableStateOf(listOf<Int>()) }
    val washroomAssignment = remember { mutableStateMapOf<Int, Int>() }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val steps = listOf("Room", "Members", "Order", "Washrooms")

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        StepHeader(steps = steps, currentStep = step)

        when (step) {

            // ── Step 0: Room name ──────────────────────────────────────────
            0 -> Column(
                Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text("Name your home 🏠", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Text("This is what your roommates will see when they join.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = roomName, onValueChange = { roomName = it },
                    label = { Text("Room / Flat name") }, leadingIcon = { Icon(Icons.Filled.Home, null) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("e.g. 2BHK Koramangala") },
                )
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { step = 1 },
                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
                    enabled = roomName.isNotBlank(),
                ) { Text("Next →", fontWeight = FontWeight.Bold) }
            }

            // ── Step 1: Add members ───────────────────────────────────────
            1 -> AddMembersStep(
                members = members,
                onNext = { masterOrder = members.indices.toList(); step = 2 },
                onBack = { step = 0 },
            )

            // ── Step 2: Rotation order ────────────────────────────────────
            2 -> Column(Modifier.fillMaxSize().padding(24.dp)) {
                Text("Rotation order 🔄", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(8.dp))
                Text("This defines who goes first in cooking & trash. Tap ▲▼ to reorder.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))

                val orderedMembers = masterOrder.map { members[it] }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(orderedMembers) { index, member ->
                        OrderRow(
                            label = ('A' + index).toString(),
                            name = member.name,
                            canMoveUp = index > 0,
                            canMoveDown = index < orderedMembers.size - 1,
                            onMoveUp = {
                                val o = masterOrder.toMutableList()
                                val t = o[index]; o[index] = o[index - 1]; o[index - 1] = t
                                masterOrder = o
                            },
                            onMoveDown = {
                                val o = masterOrder.toMutableList()
                                val t = o[index]; o[index] = o[index + 1]; o[index + 1] = t
                                masterOrder = o
                            },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { step = 1 }, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(12.dp)) { Text("← Back") }
                    Button(onClick = { step = 3 }, Modifier.weight(2f).height(52.dp), shape = RoundedCornerShape(12.dp)) { Text("Next →", fontWeight = FontWeight.Bold) }
                }
            }

            // ── Step 3: Washroom groups ───────────────────────────────────
            3 -> Column(Modifier.fillMaxSize().padding(24.dp)) {
                Text("Washroom groups 🚿", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(8.dp))
                Text("Assign each person to Washroom 1 or 2.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))

                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(members) { idx, member ->
                        val group = washroomAssignment.getOrDefault(idx, if (idx % 2 == 0) 1 else 2)
                        washroomAssignment[idx] = group
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(member.name, style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(1, 2).forEach { g ->
                                    FilterChip(selected = group == g, onClick = { washroomAssignment[idx] = g }, label = { Text("W$g") })
                                }
                            }
                        }
                    }
                }

                errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { step = 2 }, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(12.dp)) { Text("← Back") }
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true; errorMsg = null
                                try {
                                    // ── Get current user from Supabase Auth ──
                                    val authUser = SupabaseClient.client.auth.currentUserOrNull()
                                        ?: throw IllegalStateException("Not logged in")
                                    val adminUid = authUser.id
                                    val adminEmail = authUser.email ?: ""

                                    // Ensure admin is in the members list
                                    if (members.none { it.uid == adminUid }) {
                                        members.add(0, MemberDraft(adminUid, authUser.email?.substringBefore("@") ?: "Me", adminEmail))
                                        masterOrder = members.indices.toList()
                                    }

                                    val orderedUids = masterOrder.map { members[it].uid }
                                    val washroomGroups = mapOf(
                                        "1" to members.indices.filter { washroomAssignment[it] == 1 }.map { members[it].uid },
                                        "2" to members.indices.filter { washroomAssignment[it] == 2 }.map { members[it].uid },
                                    )
                                    val userObjects = members.mapIndexed { i, draft ->
                                        User(
                                            uid = draft.uid,
                                            name = draft.name,
                                            email = draft.email,
                                            masterOrder = masterOrder.indexOf(i),
                                            washroomGroup = washroomAssignment[i] ?: 1,
                                        )
                                    }
                                    repo.createRoom(
                                        roomName = roomName,
                                        adminUid = adminUid,
                                        members = userObjects,
                                        masterOrder = orderedUids,
                                        washroomGroups = washroomGroups,
                                    )
                                    onRoomCreated()
                                } catch (e: Exception) {
                                    errorMsg = e.message ?: "Failed to create room"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(2f).height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading,
                    ) {
                        if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Create Room 🎉", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

data class MemberDraft(val uid: String, val name: String, val email: String)

@Composable
private fun StepHeader(steps: List<String>, currentStep: Int) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        steps.forEachIndexed { i, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(
                        when { i < currentStep -> Primary; i == currentStep -> Primary.copy(0.3f); else -> MaterialTheme.colorScheme.surfaceVariant }
                    ), contentAlignment = Alignment.Center,
                ) {
                    if (i < currentStep) Icon(Icons.Filled.Check, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.background)
                    else Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = if (i == currentStep) Primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (i == currentStep) Primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AddMembersStep(members: MutableList<MemberDraft>, onNext: () -> Unit, onBack: () -> Unit) {
    var newName by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Auto-add the currently logged-in user as first member
    LaunchedEffect(Unit) {
        if (members.isEmpty()) {
            val authUser = SupabaseClient.client.auth.currentUserOrNull()
            if (authUser != null) {
                members.add(MemberDraft(
                    uid = authUser.id,
                    name = authUser.email?.substringBefore("@") ?: "Me",
                    email = authUser.email ?: "",
                ))
            }
        }
    }

    val adminUid = remember { SupabaseClient.client.auth.currentUserOrNull()?.id ?: "" }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Add roommates 👥", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text("Add everyone who lives here. You can add up to 6 members.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(members, key = { it.uid }) { member ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Primary.copy(0.15f)), contentAlignment = Alignment.Center) {
                        Text(member.name.firstOrNull()?.uppercase() ?: "?", color = Primary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(member.name, style = MaterialTheme.typography.titleSmall)
                        Text(member.email.ifBlank { "No email" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (member.uid != adminUid) {
                        IconButton(onClick = { members.remove(member) }) {
                            Icon(Icons.Filled.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text("You", style = MaterialTheme.typography.labelSmall, color = Primary)
                    }
                }
            }

            if (members.size < 6) {
                item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Add member", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(value = newName, onValueChange = { newName = it; errorMsg = null }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                        OutlinedTextField(value = newEmail, onValueChange = { newEmail = it; errorMsg = null }, label = { Text("Email (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                        errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        OutlinedButton(
                            onClick = {
                                if (newName.isBlank()) { errorMsg = "Name is required"; return@OutlinedButton }
                                val uid = "pending_${System.currentTimeMillis()}"
                                members.add(MemberDraft(uid, newName.trim(), newEmail.trim()))
                                newName = ""; newEmail = ""
                            },
                            shape = RoundedCornerShape(10.dp), modifier = Modifier.align(Alignment.End),
                        ) {
                            Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(12.dp)) { Text("← Back") }
            Button(onClick = {
                if (members.size < 2) { return@Button }
                onNext()
            }, Modifier.weight(2f).height(52.dp), shape = RoundedCornerShape(12.dp), enabled = members.size >= 2) {
                Text("Next →", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OrderRow(label: String, name: String, canMoveUp: Boolean, canMoveDown: Boolean, onMoveUp: () -> Unit, onMoveDown: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Primary.copy(0.2f)), contentAlignment = Alignment.Center) {
            Text(label, color = Primary, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(12.dp))
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(Icons.Filled.KeyboardArrowUp, "Move up", tint = if (canMoveUp) Primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f))
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(Icons.Filled.KeyboardArrowDown, "Move down", tint = if (canMoveDown) Primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// JoinRoomScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun JoinRoomScreen(onRoomJoined: () -> Unit) {
    val scope = rememberCoroutineScope()
    val repo = remember { RoomRepository() }
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Text("🔑", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text("Join a Room", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text("Enter the 6-character invite code from your roommate.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) { code = it.uppercase(); errorMsg = null } },
            label = { Text("Invite Code") },
            leadingIcon = { Icon(Icons.Filled.MeetingRoom, null) },
            singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        )
        errorMsg?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true; errorMsg = null
                    val roomId = repo.joinRoomByCode(code)
                    if (roomId != null) onRoomJoined()
                    else errorMsg = "Room not found. Check the code and try again."
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
            enabled = code.length == 6 && !isLoading,
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Join Room", fontWeight = FontWeight.Bold)
        }
    }
}
