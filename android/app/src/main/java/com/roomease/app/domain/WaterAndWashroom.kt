package com.roomease.app.domain

import com.roomease.app.data.model.User
import com.roomease.app.data.model.WashroomState
import com.roomease.app.data.model.WashroomStatus
import com.roomease.app.data.model.isEligible

/**
 * Water pair selection: always picks the 2 ACTIVE+PRESENT users
 * with the lowest waterFetchCount (tiebreak: longest ago).
 */
object WaterPairSelector {

    /**
     * Returns a pair of users who should fetch water next.
     * @throws IllegalStateException if fewer than 2 eligible users.
     */
    fun getNextPair(users: List<User>): Pair<User, User> {
        val eligible = users.filter { it.isEligible }
        check(eligible.size >= 2) { "Need at least 2 present users to fetch water" }

        val sorted = eligible.sortedWith(
            compareBy(
                { it.waterFetchCount },
                { it.lastFetchedAt?.toDate()?.time ?: 0L },
            )
        )
        return Pair(sorted[0], sorted[1])
    }
}

/**
 * Washroom rotation: fixed two-person groups, simple cycleIndex advancement.
 * No override mechanic.
 */
object WashroomEngine {

    /**
     * Returns the UID of who should clean next, considering presence.
     * If both group members are away → null (PENDING_RESUME).
     */
    fun getNextCleaner(state: WashroomState, users: List<User>): String? {
        val groupMembers = state.groupOrder.mapNotNull { uid -> users.find { it.uid == uid } }
        val eligible = groupMembers.filter { it.isEligible }
        if (eligible.isEmpty()) return null

        // Find the next eligible person starting from cycleIndex
        val n = state.groupOrder.size
        repeat(n) { offset ->
            val uid = state.groupOrder[(state.cycleIndex + offset) % n]
            if (eligible.any { it.uid == uid }) return uid
        }
        return null
    }

    /**
     * Advances the cycle after a cleaning is marked done.
     * @return Updated [WashroomState] — caller must persist to Firestore.
     */
    fun markDone(state: WashroomState): WashroomState {
        val newIndex = (state.cycleIndex + 1) % state.groupOrder.size
        return state.copy(cycleIndex = newIndex, status = WashroomStatus.ACTIVE)
    }

    /**
     * Computes the status based on current member presence.
     * Called whenever any user's presence changes.
     */
    fun computeStatus(state: WashroomState, users: List<User>): WashroomStatus {
        val anyPresent = state.groupOrder.any { uid ->
            users.find { it.uid == uid }?.isEligible == true
        }
        return if (anyPresent) WashroomStatus.ACTIVE else WashroomStatus.PENDING_RESUME
    }
}
