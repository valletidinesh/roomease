package com.roomease.app.domain

import com.roomease.app.data.model.GroupRotationState

/**
 * Pure, stateless rotation engine using a Queue-based model.
 * The person at index 0 is always the assigned one.
 */
object RotationEngine {

    /**
     * Returns the UID of the person assigned next (always index 0).
     */
    fun getAssigned(state: GroupRotationState): String? {
        return state.currentCycleOrder.firstOrNull()
    }

    /**
     * Records that [actualUserId] performed the task.
     * Moves [actualUserId] to the end of the queue.
     */
    fun markDone(state: GroupRotationState, actualUserId: String): GroupRotationState {
        require(state.currentCycleOrder.isNotEmpty()) { "currentCycleOrder must not be empty" }

        // ── Consecutive-cook guard ────────────────────────────────────────────
        if (actualUserId == state.lastActualUserId && state.currentCycleOrder.size > 1) {
            throw IllegalStateException(
                "Cannot perform task back-to-back when others are available."
            )
        }

        // ── Queue Logic: Move actual user to the end ──────────────────────────
        val workingOrder = state.currentCycleOrder.toMutableList()
        workingOrder.remove(actualUserId)
        workingOrder.add(actualUserId)

        return state.copy(
            currentCycleOrder = workingOrder,
            cycleIndex = 0, // Always 0 in queue model
            lastActualUserId = actualUserId,
            currentCycleNum = state.currentCycleNum + 1 // Tracking total turns
        )
    }

    fun buildGroupKey(eligibleUids: List<String>, masterOrder: List<String>): String {
        return eligibleUids
            .sortedBy { masterOrder.indexOf(it).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }
            .joinToString(",")
    }

    fun deriveSequence(groupKey: String, masterOrder: List<String>): List<String> {
        val members = groupKey.split(",").toSet()
        return masterOrder.filter { it in members }
    }

    fun createFreshState(groupKey: String, masterOrder: List<String>): GroupRotationState {
        val sequence = deriveSequence(groupKey, masterOrder)
        return GroupRotationState(
            groupKey = groupKey,
            sequence = sequence,
            currentCycleOrder = sequence.toList(),
            cycleIndex = 0,
            lastActualUserId = null,
            currentCycleNum = 1,
        )
    }
}
