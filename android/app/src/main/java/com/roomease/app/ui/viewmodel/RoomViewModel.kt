package com.roomease.app.ui.viewmodel

import androidx.lifecycle.ViewModel
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

    init {
        viewModelScope.launch {
            SupabaseClient.client.auth.sessionStatus.collect { status ->
                if (status is io.github.jan.supabase.auth.status.SessionStatus.Authenticated) {
                    loadInitialData()
                } else {
                    _currentUser.value = null
                    _room.value = null
                    _hasNoRoom.value = false
                    _isLoading.value = false
                }
            }
        }
    }

    fun refresh() {
        loadInitialData()
    }

    private fun loadInitialData() {
        _isLoading.value = true
        viewModelScope.launch {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id
            if (uid == null) {
                _isLoading.value = false
                return@launch
            }
            
            // Fetch current user row to get roomId
            val me = roomRepo.getUser(uid)
            if (me == null || me.roomId.isBlank()) {
                _hasNoRoom.value = true
                _isLoading.value = false
                _currentUser.value = me ?: User(uid = uid, email = SupabaseClient.client.auth.currentUserOrNull()?.email ?: "")
                return@launch
            }
            _currentUser.value = me
            _hasNoRoom.value = false

            // Fetch room details
            _room.value = roomRepo.getRoom(me.roomId)

            _isLoading.value = false

            // Listen to all users in the room
            launch {
                roomRepo.listenToUsers(me.roomId).collect { usersList ->
                    _users.value = usersList
                    _currentUser.value = usersList.find { it.uid == uid }
                }
            }

            // Listen to Buy List
            SupabaseClient.client.from("buy_list")
                .selectAsFlow<com.roomease.app.data.model.BuyListItem>(
                    com.roomease.app.data.model.BuyListItem::id
                ).onEach { list: List<com.roomease.app.data.model.BuyListItem> ->
                    _buyList.value = list.filter { it.roomId == me.roomId }
                }.launchIn(this)

            // Listen to Rotation States
            SupabaseClient.client.from("group_rotation_state")
                .selectAsFlow<com.roomease.app.data.model.GroupRotationState>(
                    com.roomease.app.data.model.GroupRotationState::id
                ).onEach { list: List<com.roomease.app.data.model.GroupRotationState> ->
                    _rotationStates.value = list.filter { it.roomId == me.roomId }.associateBy { it.groupKey }
                }.launchIn(this)

            // Listen to Consumables (Purchase Entries)
            SupabaseClient.client.from("purchase_entries")
                .selectAsFlow<com.roomease.app.data.model.PurchaseEntry>(
                    com.roomease.app.data.model.PurchaseEntry::id
                ).onEach { list: List<com.roomease.app.data.model.PurchaseEntry> ->
                    _consumables.value = list.filter { it.roomId == me.roomId }
                }.launchIn(this)

            // Listen to Usage Logs
            SupabaseClient.client.from("usage_logs")
                .selectAsFlow<com.roomease.app.data.model.UsageLog>(
                    com.roomease.app.data.model.UsageLog::id
                ).onEach { list: List<com.roomease.app.data.model.UsageLog> ->
                    _usageLogs.value = list.filter { it.roomId == me.roomId }.groupBy { it.purchaseEntryId }
                }.launchIn(this)

            // Listen to Washroom States
            SupabaseClient.client.from("washroom_state")
                .selectAsFlow<com.roomease.app.data.model.WashroomState>(
                    com.roomease.app.data.model.WashroomState::id
                ).onEach { list: List<com.roomease.app.data.model.WashroomState> ->
                    _washroomStates.value = list.filter { it.roomId == me.roomId }.associateBy { it.washroomNumber }
                }.launchIn(this)
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
