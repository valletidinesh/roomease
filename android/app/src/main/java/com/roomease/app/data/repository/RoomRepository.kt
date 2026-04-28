package com.roomease.app.data.repository

import com.roomease.app.SupabaseClient
import com.roomease.app.data.model.Room
import com.roomease.app.data.model.User
import com.roomease.app.data.model.WashroomState
import com.roomease.app.domain.RotationEngine
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.add
import kotlinx.serialization.encodeToString
import com.roomease.app.data.model.GroupRotationState

class RoomRepository {

    private val db get() = SupabaseClient.client

    // ── Room CRUD ─────────────────────────────────────────────────────────────

    suspend fun createRoom(
        roomName: String,
        adminUid: String,
        members: List<User>,
        masterOrder: List<String>,
        washroomGroups: Map<String, List<String>>,
    ): String {
        val inviteCode = generateInviteCode()

        // 1. Insert room
        val room = db.from("rooms").insert(
            buildJsonObject {
                put("name", roomName)
                put("admin_uid", adminUid)
                put("invite_code", inviteCode)
                put("master_order", buildJsonArray { masterOrder.forEach { add(it) } })
                val groupsObj = buildJsonObject {
                    washroomGroups.forEach { (k, v) -> put(k, buildJsonArray { v.forEach { add(it) } }) }
                }
                put("washroom_groups", groupsObj)
            }
        ) { select() }.decodeSingle<Room>()

        val roomId = room.id

        // 2. Insert all members
        val userRows = members.map { u -> u.copy(roomId = roomId) }
        db.from("users").insert(userRows)

        // 3. Seed default rotation states for all modules
        val defaultStates = mutableListOf<GroupRotationState>()
        
        // Modules using masterOrder indices directly
        val modules = listOf("TRASH_WET", "TRASH_DRY", "WATER")
        modules.forEach { key ->
            defaultStates.add(
                GroupRotationState(
                    roomId = roomId,
                    groupKey = key,
                    sequence = masterOrder,
                    currentCycleOrder = masterOrder,
                    cycleIndex = 0
                )
            )
        }
        db.from("group_rotation_state").insert(defaultStates)

        // 4. Seed washroom states (e.g. Washroom 1 and Washroom 2)
        washroomGroups.forEach { (numStr, uids) ->
            db.from("washroom_state").insert(
                buildJsonObject {
                    put("room_id", roomId)
                    put("washroom_number", numStr.toInt())
                    // The rotation is between "1" and "2" (the group IDs)
                    put("group_order", buildJsonArray { add("1"); add("2") })
                    put("cycle_index", 0)
                    put("status", "ACTIVE")
                }
            )
        }

        // 5. Pre-seed all possible cooking group combinations
        val cookingStates = generateAllGroupStates(roomId, masterOrder)
        db.from("group_rotation_state").insert(cookingStates)

        return roomId
    }

    suspend fun joinRoomByCode(inviteCode: String): String? {
        val rooms = db.from("rooms")
            .select { filter { eq("invite_code", inviteCode.uppercase()) } }
            .decodeList<Room>()
        return rooms.firstOrNull()?.id
    }

    suspend fun getRoom(roomId: String): Room {
        return db.from("rooms")
            .select { filter { eq("id", roomId) } }
            .decodeSingle<Room>()
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    suspend fun getUser(uid: String): User? {
        return db.from("users")
            .select { filter { eq("uid", uid) } }
            .decodeSingleOrNull<User>()
    }

    suspend fun getUsers(roomId: String): List<User> {
        return db.from("users")
            .select { filter { eq("room_id", roomId) } }
            .decodeList<User>()
            .sortedBy { it.masterOrder }
    }

    /**
     * Real-time listener for user list changes.
     */
    fun listenToUsers(roomId: String): Flow<List<User>> = flow {
        val channel = db.realtime.channel("users-$roomId")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "users"
            filter(FilterOperation("room_id", FilterOperator.EQ, roomId))
        }

        channel.subscribe()
        emit(getUsers(roomId))
        changes.collect { emit(getUsers(roomId)) }
    }

    suspend fun updateStatus(userId: String, status: String) {
        db.from("users").update({ set("status", status) }) {
            filter { eq("uid", userId) }
        }
    }
 
    suspend fun updateUserName(userId: String, newName: String) {
        db.from("users").update({ set("name", newName) }) {
            filter { eq("uid", userId) }
        }
    }
 
    suspend fun updatePresence(userId: String, presence: String) {
        db.from("users").update({ set("presence", presence) }) {
            filter { eq("uid", userId) }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    /**
     * Generates all non-empty subsets of masterOrder as group rotation state rows.
     */
    private fun generateAllGroupStates(
        roomId: String,
        masterOrder: List<String>,
    ): List<GroupRotationState> {
        val result = mutableListOf<GroupRotationState>()
        val n = masterOrder.size
        for (mask in 1 until (1 shl n)) {
            val group = masterOrder.filterIndexed { i, _ -> (mask shr i) and 1 == 1 }
            val groupKey = group.joinToString(",")
            result.add(GroupRotationState(
                roomId = roomId,
                groupKey = groupKey,
                sequence = group,
                currentCycleOrder = group,
                cycleIndex = 0,
                lastActualUserId = null,
                currentCycleNum = 1
            ))
        }
        return result
    }
}
