package com.roomease.app.data.repository

import com.roomease.app.SupabaseClient
import com.roomease.app.data.model.*
import com.roomease.app.domain.TrashSelector
import com.roomease.app.domain.WashroomEngine
import com.roomease.app.domain.WaterPairSelector
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

// ─────────────────────────────────────────────────────────────────────────────
// TrashRepository
// ─────────────────────────────────────────────────────────────────────────────
class TrashRepository {
    private val db get() = SupabaseClient.client

    fun getNextThrower(users: List<User>, trashType: TrashType): User =
        TrashSelector.getNextThrower(users, trashType)

    suspend fun markDone(roomId: String, userId: String, trashType: TrashType) {
        val user = db.from("users")
            .select { filter { eq("uid", userId); eq("room_id", roomId) } }
            .decodeSingle<User>()

        val newWet = if (trashType == TrashType.WET) user.trashWetCount + 1 else user.trashWetCount
        val newDry = if (trashType == TrashType.DRY) user.trashDryCount + 1 else user.trashDryCount
        val completeTurnsAfter = minOf(newWet, newDry)

        // Update user counts
        db.from("users").update(
            {
                set("trash_wet_count", newWet)
                set("trash_dry_count", newDry)
            }
        ) { filter { eq("uid", userId); eq("room_id", roomId) } }

        // Append history
        db.from("trash_history").insert(
            TrashHistory(
                roomId = roomId,
                userId = userId,
                trashType = trashType.name,
                completeTurnsAfter = completeTurnsAfter
            )
        )
    }

    fun listenToTrashHistory(roomId: String): Flow<List<TrashHistory>> = flow {
        val channel = db.realtime.channel("trash-$roomId")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "trash_history"
            filter(FilterOperation("room_id", FilterOperator.EQ, roomId))
        }
        channel.subscribe()
        
        val fetch = suspend {
            db.from("trash_history")
                .select { filter { eq("room_id", roomId) }; order("thrown_at", Order.DESCENDING); limit(30) }
                .decodeList<TrashHistory>()
        }
        emit(fetch())
        changes.collect { emit(fetch()) }
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
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "washroom_state"
            filter(FilterOperation("room_id", FilterOperator.EQ, roomId))
        }
        channel.subscribe()
        emit(getWashroomState(roomId, washroomNumber))
        changes.collect { emit(getWashroomState(roomId, washroomNumber)) }
    }

    suspend fun markCleaned(roomId: String, washroomNumber: Int) {
        val state = getWashroomState(roomId, washroomNumber)
        val newState = WashroomEngine.markDone(state)
        db.from("washroom_state").update(
            {
                set("cycle_index", newState.cycleIndex)
                set("status", newState.status)
            }
        ) { filter { eq("room_id", roomId); eq("washroom_number", washroomNumber) } }
    }

    suspend fun refreshStatus(roomId: String, washroomNumber: Int, users: List<User>) {
        val state = getWashroomState(roomId, washroomNumber)
        val newStatus = WashroomEngine.computeStatus(state, users)
        if (newStatus != state.status) {
            db.from("washroom_state").update(
                { set("status", newStatus.name) }
            ) {
                filter { eq("room_id", roomId); eq("washroom_number", washroomNumber) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WaterRepository
// ─────────────────────────────────────────────────────────────────────────────
class WaterRepository {
    private val db get() = SupabaseClient.client

    fun getNextPair(users: List<User>): Pair<User, User> =
        WaterPairSelector.getNextPair(users)

    suspend fun markDone(roomId: String, users: List<User>) {
        val (user1, user2) = WaterPairSelector.getNextPair(users)

        listOf(user1, user2).forEach { u ->
            db.from("users").update(
                {
                    set("water_fetch_count", u.waterFetchCount + 1)
                }
            ) { filter { eq("uid", u.uid); eq("room_id", roomId) } }
        }

        db.from("water_history").insert(
            WaterHistory(
                roomId = roomId,
                pair = listOf(user1.uid, user2.uid)
            )
        )
    }

    fun listenToWaterHistory(roomId: String): Flow<List<WaterHistory>> = flow {
        val channel = db.realtime.channel("water-$roomId")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "water_history"
            filter(FilterOperation("room_id", FilterOperator.EQ, roomId))
        }
        channel.subscribe()
        
        val fetch = suspend {
            db.from("water_history")
                .select { filter { eq("room_id", roomId) }; order("fetched_at", Order.DESCENDING); limit(20) }
                .decodeList<WaterHistory>()
        }
        emit(fetch())
        changes.collect { emit(fetch()) }
    }
}
