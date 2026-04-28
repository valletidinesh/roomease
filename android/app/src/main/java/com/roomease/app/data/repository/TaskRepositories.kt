package com.roomease.app.data.repository

import com.roomease.app.SupabaseClient
import com.roomease.app.data.model.*
import com.roomease.app.domain.RotationEngine
import com.roomease.app.domain.WashroomEngine
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
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject

// ─────────────────────────────────────────────────────────────────────────────
// TrashRepository
// ─────────────────────────────────────────────────────────────────────────────
class TrashRepository {
    private val db get() = SupabaseClient.client

    suspend fun getRotationState(roomId: String, trashType: TrashType): GroupRotationState {
        val key = if (trashType == TrashType.WET) "TRASH_WET" else "TRASH_DRY"
        return db.from("group_rotation_state")
            .select { filter { eq("room_id", roomId); eq("group_key", key) } }
            .decodeSingle<GroupRotationState>()
    }

    suspend fun markDone(roomId: String, userId: String, trashType: TrashType) {
        val state = getRotationState(roomId, trashType)
        val newState = RotationEngine.markDone(state, userId)
        
        // Update rotation state
        db.from("group_rotation_state").update(
            buildJsonObject {
                put("current_cycle_order", buildJsonArray { newState.currentCycleOrder.forEach { add(it) } })
                put("cycle_index", newState.cycleIndex)
                put("last_actual_user_id", newState.lastActualUserId)
                put("current_cycle_num", newState.currentCycleNum)
            }
        ) { filter { eq("id", state.id) } }

        // Update user counts
        val user = db.from("users").select { filter { eq("uid", userId) } }.decodeSingle<User>()
        val newWet = if (trashType == TrashType.WET) user.trashWetCount + 1 else user.trashWetCount
        val newDry = if (trashType == TrashType.DRY) user.trashDryCount + 1 else user.trashDryCount
        
        db.from("users").update({
            set("trash_wet_count", newWet)
            set("trash_dry_count", newDry)
        }) { filter { eq("uid", userId) } }

        // History
        db.from("trash_history").insert(
            TrashHistory(roomId = roomId, userId = userId, trashType = trashType.name, completeTurnsAfter = minOf(newWet, newDry))
        )
    }

    fun listenToTrashHistory(roomId: String): Flow<List<TrashHistory>> = flow {
        val channel = db.realtime.channel("trash-$roomId")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "trash_history" }
        channel.subscribe()
        val fetch = suspend { db.from("trash_history").select { filter { eq("room_id", roomId) }; order("thrown_at", Order.DESCENDING); limit(30) }.decodeList<TrashHistory>() }
        emit(fetch()); changes.collect { emit(fetch()) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WashroomRepository
// ─────────────────────────────────────────────────────────────────────────────
class WashroomRepository {
    private val db get() = SupabaseClient.client

    suspend fun getWashroomState(roomId: String, washroomNumber: Int): WashroomState {
        return db.from("washroom_state")
            .select { filter { eq("room_id", roomId); eq("washroom_number", washroomNumber) } }
            .decodeSingle()
    }

    fun listenToWashroomState(roomId: String, washroomNumber: Int): Flow<WashroomState> = flow {
        val channel = db.realtime.channel("washroom-$roomId-$washroomNumber")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "washroom_state" }
        channel.subscribe()
        emit(getWashroomState(roomId, washroomNumber))
        changes.collect { emit(getWashroomState(roomId, washroomNumber)) }
    }

    suspend fun markCleaned(roomId: String, washroomNumber: Int) {
        val state = getWashroomState(roomId, washroomNumber)
        val newIndex = (state.cycleIndex + 1) % state.groupOrder.size
        db.from("washroom_state").update({
            set("cycle_index", newIndex)
            set("status", "CLEAN")
        }) { filter { eq("room_id", roomId); eq("washroom_number", washroomNumber) } }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WaterRepository
// ─────────────────────────────────────────────────────────────────────────────
class WaterRepository {
    private val db get() = SupabaseClient.client

    suspend fun getRotationState(roomId: String): GroupRotationState {
        return db.from("group_rotation_state")
            .select { filter { eq("room_id", roomId); eq("group_key", "WATER") } }
            .decodeSingle<GroupRotationState>()
    }

    suspend fun markDone(roomId: String, users: List<User>) {
        val state = getRotationState(roomId)
        val masterOrder = state.sequence
        
        // Pick the next 2 from the current cycle order
        val pairUids = state.currentCycleOrder.take(2)
        
        // Update rotation state: move BOTH to end
        var workingOrder = state.currentCycleOrder.toMutableList()
        pairUids.forEach { uid ->
            workingOrder.remove(uid)
            workingOrder.add(uid)
        }

        db.from("group_rotation_state").update(
            buildJsonObject {
                put("current_cycle_order", buildJsonArray { workingOrder.forEach { add(it) } })
                put("cycle_index", 0)
                put("current_cycle_num", state.currentCycleNum + 1)
            }
        ) { filter { eq("id", state.id) } }

        // Update user counts
        pairUids.forEach { uid ->
            val u = users.find { it.uid == uid } ?: return@forEach
            db.from("users").update({
                set("water_fetch_count", u.waterFetchCount + 1)
            }) { filter { eq("uid", uid) } }
        }

        db.from("water_history").insert(WaterHistory(roomId = roomId, pair = pairUids))
    }

    fun listenToWaterHistory(roomId: String): Flow<List<WaterHistory>> = flow {
        val channel = db.realtime.channel("water-$roomId")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "water_history" }
        channel.subscribe()
        val fetch = suspend { db.from("water_history").select { filter { eq("room_id", roomId) }; order("fetched_at", Order.DESCENDING); limit(20) }.decodeList<WaterHistory>() }
        emit(fetch()); changes.collect { emit(fetch()) }
    }
}
