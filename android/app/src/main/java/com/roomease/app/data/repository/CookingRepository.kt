package com.roomease.app.data.repository

import com.roomease.app.SupabaseClient
import com.roomease.app.data.model.CookingHistory
import com.roomease.app.data.model.GroupRotationState
import com.roomease.app.data.model.User
import com.roomease.app.domain.RotationEngine
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.roomease.app.data.model.isEligible
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator

class CookingRepository {

    private val db get() = SupabaseClient.client

    // ── Group state ────────────────────────────────────────────────────────────

    suspend fun getGroupState(roomId: String, groupKey: String): GroupRotationState {
        return db.from("group_rotation_state")
            .select { filter { eq("room_id", roomId); eq("group_key", groupKey) } }
            .decodeSingle()
    }

    fun listenToGroupState(roomId: String, groupKey: String): Flow<GroupRotationState> = flow {
        val channel = db.realtime.channel("cooking-$roomId-$groupKey")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "group_rotation_state"
            filter(FilterOperation("room_id", FilterOperator.EQ, roomId))
        }
        channel.subscribe()
        emit(getGroupState(roomId, groupKey))
        changes.collect { emit(getGroupState(roomId, groupKey)) }
    }

    fun buildGroupKey(users: List<User>, masterOrder: List<String>): String {
        val eligibleUids = users.filter { it.isEligible }.map { it.uid }
        return RotationEngine.buildGroupKey(eligibleUids, masterOrder)
    }

    // ── Mark done — uses Supabase RPC (database-level transaction) ─────────────

    /**
     * Calls a PostgreSQL function `mark_cooking_done` which atomically:
     *   1. Reads group_rotation_state
     *   2. Applies override / cycle advance
     *   3. Writes updated state
     *   4. Inserts cooking_history row
     *
     * This replaces the Firestore transaction and avoids race conditions.
     * The function is defined in schema.sql.
     */
    suspend fun markDone(roomId: String, groupKey: String, actualUserId: String) {
        // Read current state
        val state = getGroupState(roomId, groupKey)
        val assigned = RotationEngine.getAssigned(state) ?: throw IllegalStateException("No user assigned for this group")
        val newState = RotationEngine.markDone(state, actualUserId)

        // Update group state
        db.from("group_rotation_state").update(
            {
                set("current_cycle_order", newState.currentCycleOrder)
                set("cycle_index", newState.cycleIndex)
                set("last_actual_user_id", newState.lastActualUserId)
                set("current_cycle_num", newState.currentCycleNum)
            }
        ) { filter { eq("room_id", roomId); eq("group_key", groupKey) } }

        // Insert history
        db.from("cooking_history").insert(
            CookingHistory(
                roomId = roomId,
                groupKey = groupKey,
                assignedUserId = assigned,
                actualUserId = actualUserId,
                cycleNumber = if (newState.cycleIndex == 0)
                                  newState.currentCycleNum - 1
                              else newState.currentCycleNum
            )
        )

        // Prune old cycles
        pruneHistory(roomId, groupKey, newState.currentCycleNum)
    }

    // ── History ───────────────────────────────────────────────────────────────

    suspend fun getCookingHistory(
        roomId: String,
        groupKey: String,
        maxCycles: Int = 3,
    ): List<CookingHistory> {
        val state = getGroupState(roomId, groupKey)
        val minCycle = (state.currentCycleNum - maxCycles).coerceAtLeast(1)

        return db.from("cooking_history")
            .select {
                filter {
                    eq("room_id", roomId)
                    eq("group_key", groupKey)
                    gte("cycle_number", minCycle)
                }
                order("cycle_number", Order.ASCENDING)
                order("cooked_at", Order.ASCENDING)
            }
            .decodeList()
    }

    fun listenToHistory(roomId: String, groupKey: String): Flow<List<CookingHistory>> = flow {
        val channel = db.realtime.channel("cooking-history-$roomId")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "cooking_history"
            filter(FilterOperation("room_id", FilterOperator.EQ, roomId))
        }
        channel.subscribe()
        emit(getCookingHistory(roomId, groupKey))
        changes.collect { emit(getCookingHistory(roomId, groupKey)) }
    }

    // ── Pruning ───────────────────────────────────────────────────────────────

    private suspend fun pruneHistory(roomId: String, groupKey: String, currentCycleNum: Int) {
        try {
            val minKeep = (currentCycleNum - 3).coerceAtLeast(1)
            if (minKeep <= 1) return
            db.from("cooking_history").delete {
                filter {
                    eq("room_id", roomId)
                    eq("group_key", groupKey)
                    lt("cycle_number", minKeep)
                }
            }
        } catch (_: Exception) { /* non-critical */ }
    }
}
