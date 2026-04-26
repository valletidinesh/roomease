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

class RoomViewModel : ViewModel() {
    private val roomRepo = RoomRepository()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _room = MutableStateFlow<Room?>(null)
    val room: StateFlow<Room?> = _room.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _hasNoRoom = MutableStateFlow(false)
    val hasNoRoom: StateFlow<Boolean> = _hasNoRoom.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id
            if (uid == null) {
                _isLoading.value = false
                return@launch
            }
            
            // Fetch current user row to get roomId
            val me = roomRepo.getUser(uid)
            if (me == null) {
                _hasNoRoom.value = true
                _isLoading.value = false
                return@launch
            }
            _currentUser.value = me

            // Fetch room details
            _room.value = roomRepo.getRoom(me.roomId)

            _isLoading.value = false

            // Listen to all users in the room
            roomRepo.listenToUsers(me.roomId).collect { usersList ->
                _users.value = usersList
                // Update current user if it changed in the list
                _currentUser.value = usersList.find { it.uid == uid }
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
