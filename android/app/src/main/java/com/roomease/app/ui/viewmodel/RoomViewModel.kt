package com.roomease.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import io.github.jan.supabase.annotations.SupabaseExperimental
import androidx.lifecycle.viewModelScope
import com.roomease.app.SupabaseClient
import com.roomease.app.data.model.Room
import com.roomease.app.data.model.User
import com.roomease.app.data.repository.RoomRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.flow.*
import io.github.jan.supabase.postgrest.query.filter.*
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.Job

@OptIn(SupabaseExperimental::class)
class RoomViewModel : ViewModel() {
    private val roomRepo = RoomRepository()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _room = MutableStateFlow<Room?>(null)
    val room: StateFlow<Room?> = _room.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _buyList = MutableStateFlow<List<com.roomease.app.data.model.BuyListItem>>(emptyList())
    val buyList: StateFlow<List<com.roomease.app.data.model.BuyListItem>> = _buyList.asStateFlow()

    private val _consumables = MutableStateFlow<List<com.roomease.app.data.model.PurchaseEntry>>(emptyList())
    val consumables: StateFlow<List<com.roomease.app.data.model.PurchaseEntry>> = _consumables.asStateFlow()

    private val _usageLogs = MutableStateFlow<Map<String, List<com.roomease.app.data.model.UsageLog>>>(emptyMap())
    val usageLogs: StateFlow<Map<String, List<com.roomease.app.data.model.UsageLog>>> = _usageLogs.asStateFlow()

    private val _washroomStates = MutableStateFlow<Map<Int, com.roomease.app.data.model.WashroomState>>(emptyMap())
    val washroomStates: StateFlow<Map<Int, com.roomease.app.data.model.WashroomState>> = _washroomStates.asStateFlow()

    private val _rotationStates = MutableStateFlow<Map<String, com.roomease.app.data.model.GroupRotationState>>(emptyMap())
    val rotationStates: StateFlow<Map<String, com.roomease.app.data.model.GroupRotationState>> = _rotationStates.asStateFlow()

    private val _hasNoRoom = MutableStateFlow(false)
    val hasNoRoom: StateFlow<Boolean> = _hasNoRoom.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var refreshJob: Job? = null
    private var listenersJob: Job? = null

    init {
        viewModelScope.launch {
            SupabaseClient.client.auth.sessionStatus.collect { status ->
                if (status is io.github.jan.supabase.auth.status.SessionStatus.Authenticated) {
                    refresh()
                } else {
                    _isLoading.value = false
                    _currentUser.value = null
                    _room.value = null
                    listenersJob?.cancel()
                }
            }
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                val uid = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (uid == null) {
                    _isLoading.value = false
                    return@launch
                }
                
                // Fetch current user row to get roomId
                val me = try { roomRepo.getUser(uid) } catch (e: Exception) { null }
                if (me == null || me.roomId.isBlank()) {
                    _hasNoRoom.value = true
                    _isLoading.value = false
                    _currentUser.value = me ?: User(uid = uid, email = SupabaseClient.client.auth.currentUserOrNull()?.email ?: "")
                    listenersJob?.cancel()
                    return@launch
                }
                _currentUser.value = me
                _hasNoRoom.value = false

                // Fetch room details
                _room.value = try { roomRepo.getRoom(me.roomId) } catch (e: Exception) { null }

                _isLoading.value = false

                // Restart listeners
                listenersJob?.cancel()
                listenersJob = launch {
                    // Listen to all users in the room
                    launch {
                        try {
                            roomRepo.listenToUsers(me.roomId).collect { usersList ->
                                _users.value = usersList
                                _currentUser.value = usersList.find { it.uid == uid }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }

                    // Listen to Buy List
                    try {
                        SupabaseClient.client.from("buy_list")
                            .selectAsFlow(
                                com.roomease.app.data.model.BuyListItem::id,
                                filter = FilterOperation("room_id", FilterOperator.EQ, me.roomId)
                            ).onEach { list -> _buyList.value = list }
                            .launchIn(this)
                    } catch (e: Exception) { e.printStackTrace() }

                    // Listen to Rotation States
                    try {
                        SupabaseClient.client.from("group_rotation_state")
                            .selectAsFlow(
                                com.roomease.app.data.model.GroupRotationState::id,
                                filter = FilterOperation("room_id", FilterOperator.EQ, me.roomId)
                            ).onEach { list -> _rotationStates.value = list.associateBy { it.groupKey } }
                            .launchIn(this)
                    } catch (e: Exception) { e.printStackTrace() }

                    // Listen to Consumables (Purchase Entries)
                    try {
                        SupabaseClient.client.from("purchase_entries")
                            .selectAsFlow(
                                com.roomease.app.data.model.PurchaseEntry::id,
                                filter = FilterOperation("room_id", FilterOperator.EQ, me.roomId)
                            ).onEach { list -> _consumables.value = list }
                            .launchIn(this)
                    } catch (e: Exception) { e.printStackTrace() }

                    // Listen to Usage Logs
                    try {
                        SupabaseClient.client.from("usage_logs")
                            .selectAsFlow(
                                com.roomease.app.data.model.UsageLog::id,
                                filter = FilterOperation("room_id", FilterOperator.EQ, me.roomId)
                            ).onEach { list -> _usageLogs.value = list.groupBy { it.purchaseEntryId } }
                            .launchIn(this)
                    } catch (e: Exception) { e.printStackTrace() }

                    // Listen to Washroom States
                    try {
                        SupabaseClient.client.from("washroom_state")
                            .selectAsFlow(
                                com.roomease.app.data.model.WashroomState::id,
                                filter = FilterOperation("room_id", FilterOperator.EQ, me.roomId)
                            ).onEach { list -> _washroomStates.value = list.associateBy { it.washroomNumber } }
                            .launchIn(this)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    fun updatePresence(isPresent: Boolean) {
        val me = _currentUser.value ?: return
        viewModelScope.launch {
            val status = if (isPresent) "PRESENT" else "AWAY"
            roomRepo.updatePresence(me.roomId, me.uid, status)
        }
    }

    fun updateName(newName: String) {
        val me = _currentUser.value ?: return
        viewModelScope.launch {
            roomRepo.updateUserName(me.roomId, me.uid, newName)
        }
    }
}
