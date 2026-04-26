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
import kotlinx.serialization.json.put
import kotlinx.serialization.encodeToString

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
            mapOf(
                "name"            to roomName,
                "admin_uid"       to adminUid,
                "invite_code"     to inviteCode,
                "master_order"    to masterOrder,
                "washroom_groups" to washroomGroups,
            )
        ) { select() }.decodeSingle<Room>()

        val roomId = room.id

        // 2. Insert all members
        val userRows = members.map { u ->
            mapOf(
                "uid"               to u.uid,
                "room_id"           to roomId,
                "name"              to u.name,
                "email"             to u.email,
                "phone"             to u.phone,
                "master_order"      to u.masterOrder,
                "status"            to "ACTIVE",
                "presence"          to "PRESENT",
                "washroom_group"    to u.washroomGroup,
                "water_fetch_count" to 0,
                "trash_wet_count"   to 0,
                "trash_dry_count"   to 0,
            )
        }
        db.from("users").insert(userRows)

        // 3. Pre-seed all group rotation states
        val groupStates = generateAllGroupStates(roomId, masterOrder)
        db.from("group_rotation_state").insert(groupStates)

        // 4. Seed washroom states
        val washroomRows = washroomGroups.map { (number, uids) ->
            mapOf(
                "room_id"         to roomId,
                "washroom_number" to number.toInt(),
                "group_order"     to uids,
                "cycle_index"     to 0,
                "status"          to "ACTIVE",
            )
        }
        db.from("washroom_state").insert(washroomRows)

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

    suspend fun getUsers(roomId: String): List<User> {
        return db.from("users")
            .select { filter { eq("room_id", roomId) } }
            .decodeList<User>()
            .sortedBy { it.masterOrder }
    }

    /**
     * Real-time listener for user list changes.
     * Supabase Realtime broadcasts row-level changes via PostgreSQL logical replication.
     */
    fun listenToUsers(roomId: String): Flow<List<User>> = flow {
        val channel = db.realtime.channel("users-$roomId")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "users"
            filter(FilterOperation("room_id", FilterOperator.EQ, roomId))
        }

        // Subscribe in background
        channel.subscribe()
        emit(getUsers(roomId))

        // On any change, re-fetch the full list
        changes.collect { emit(getUsers(roomId)) }
    }

    suspend fun updatePresence(roomId: String, userId: String, presence: String) {
        db.from("users").update(mapOf("presence" to presence)) {
            filter { eq("uid", userId); eq("room_id", roomId) }
        }
    }

    suspend fun updateStatus(roomId: String, userId: String, status: String) {
        db.from("users").update(mapOf("status" to status)) {
            filter { eq("uid", userId); eq("room_id", roomId) }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    /**
     * Generates all non-empty subsets of masterOrder as group rotation state rows.
     * For 4 members = 15 subsets (sizes 1–4).
     */
    private fun generateAllGroupStates(
        roomId: String,
        masterOrder: List<String>,
    ): List<Map<String, Any?>> {
        val result = mutableListOf<Map<String, Any?>>()
        val n = masterOrder.size
        for (mask in 1 until (1 shl n)) {
            val group = masterOrder.filterIndexed { i, _ -> (mask shr i) and 1 == 1 }
            val groupKey = group.joinToString(",")
            result.add(mapOf(
                "room_id"             to roomId,
                "group_key"           to groupKey,
                "sequence"            to group,
                "current_cycle_order" to group,
                "cycle_index"         to 0,
                "last_actual_user_id" to null,
                "current_cycle_num"   to 1,
            ))
        }
        return result
    }
}
