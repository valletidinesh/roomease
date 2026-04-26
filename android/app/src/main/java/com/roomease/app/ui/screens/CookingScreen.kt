package com.roomease.app.ui.screens

import androidx.compose.foundation.background
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
    val scope = rememberCoroutineScope()
    val cookingRepo = remember { CookingRepository() }
    val roomRepo = remember { RoomRepository() }

    // TODO (Phase 2): wire roomId from DataStore / shared ViewModel
    // Placeholder state until ViewModels are wired
    var assignedName by remember { mutableStateOf("—") }
    var groupLabel by remember { mutableStateOf("") }
    var showOverridePicker by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

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
                    if (groupLabel.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = CookingColor.copy(0.1f)) {
                            Text("Group: $groupLabel", Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = CookingColor)
                        }
                    }
                }
            }

            successMsg?.let {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(0.2f)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Primary)
                        Spacer(Modifier.width(8.dp))
                        Text(it, color = Primary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            errorMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.weight(1f))

            // Mark done (self)
            Button(
                onClick = {
                    scope.launch {
                        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
                        successMsg = "Cooking marked done! 🎉"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CookingColor, contentColor = MaterialTheme.colorScheme.background),
                enabled = !isLoading,
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.background)
                else { Icon(Icons.Filled.Check, null); Spacer(Modifier.width(8.dp)); Text("Mark Done (Me)", fontWeight = FontWeight.Bold) }
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingCalendarScreen(onBack: () -> Unit) {
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
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding), contentAlignment = Alignment.Center) {
            Text("History loads here (real-time from Firestore)", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
