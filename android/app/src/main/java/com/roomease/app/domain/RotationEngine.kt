package com.roomease.app.domain

import com.roomease.app.data.model.GroupRotationState

/**
 * Pure, stateless rotation engine.
 * Contains zero Android or Firebase imports — fully unit-testable with JUnit.
 *
 * This engine is used for COOKING (group-aware with 11 possible states).
 */
object RotationEngine {

    /**
     * Returns the UID of the person assigned to cook next
     * given the current group's state.
     */
    fun getAssigned(state: GroupRotationState): String {
        require(state.currentCycleOrder.isNotEmpty()) { "currentCycleOrder must not be empty" }
        return state.currentCycleOrder[state.cycleIndex]
    }

    /**
     * Records that [actualUserId] performed the task.
     *
     * - If [actualUserId] ≠ assigned → applies override (swap positions so actual cooks now,
     *   assigned is pushed back to where actual was).
     * - Consecutive-cook guard: throws if the same person cooks back-to-back in a group of > 1.
     * - Advances cycleIndex; resets cycle when it completes.
     *
     * @return Updated [GroupRotationState] — caller must persist this to Firestore.
     * @throws IllegalStateException if same person cooks consecutively in a multi-person group.
     */
    fun markDone(state: GroupRotationState, actualUserId: String): GroupRotationState {
        require(state.currentCycleOrder.isNotEmpty()) { "currentCycleOrder must not be empty" }

        val assigned = state.currentCycleOrder[state.cycleIndex]

        // ── Consecutive-cook guard ────────────────────────────────────────────
        if (actualUserId == state.lastActualUserId && state.sequence.size > 1) {
            throw IllegalStateException(
                "Cannot cook back-to-back when others are available. " +
                "Last cook was also $actualUserId."
            )
        }

        // ── Override: someone other than assigned cooked ──────────────────────
        var workingOrder = state.currentCycleOrder.toMutableList()
        if (actualUserId != assigned) {
            workingOrder = applyOverride(workingOrder, assigned, actualUserId)
        }

        // ── Advance pointer ───────────────────────────────────────────────────
        var newIndex = state.cycleIndex + 1
        var newCycleNum = state.currentCycleNum
        var newOrder: List<String> = workingOrder

        if (newIndex >= workingOrder.size) {
            // Cycle complete — reset to canonical sequence
            newOrder = state.sequence.toList()
            newIndex = 0
            newCycleNum++
        }

        return state.copy(
            currentCycleOrder = newOrder,
            cycleIndex = newIndex,
            lastActualUserId = actualUserId,
            currentCycleNum = newCycleNum,
        )
    }

    /**
     * Override mechanic:
     * Remove [actual] from its future position in the cycle and insert it at [assigned]'s
     * current slot. The cycle index is unchanged — it now points to [actual].
     *
     * Example: order = [A, B, D], index=1 (B is next), D overrides.
     *   → remove D from index 2: [A, B]
     *   → insert D at indexOf(B)=1: [A, D, B]
     *   → cycleIndex=1 now points to D  ✓
     *   → After D is marked done, index advances to 2 → B cooks next  ✓
     */
    private fun applyOverride(
        order: MutableList<String>,
        assigned: String,
        actual: String,
    ): MutableList<String> {
        order.remove(actual)
        val insertPos = order.indexOf(assigned)
        require(insertPos >= 0) { "Assigned user $assigned not found in cycle order" }
        order.add(insertPos, actual)
        return order
    }

    /**
     * Builds the group key for a set of eligible UIDs.
     * Always sorts by master order to produce a consistent, canonical key.
     *
     * @param eligibleUids UIDs of ACTIVE + PRESENT users.
     * @param masterOrder Full ordered list of all UIDs (defines A→B→C→D).
     * @return e.g. "uid_A,uid_B,uid_D"
     */
    fun buildGroupKey(eligibleUids: List<String>, masterOrder: List<String>): String {
        return eligibleUids
            .sortedBy { masterOrder.indexOf(it).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }
            .joinToString(",")
    }

    /**
     * Derives the canonical sequence for a given group key from the master order.
     * Used when pre-seeding all 11 group state documents.
     */
    fun deriveSequence(groupKey: String, masterOrder: List<String>): List<String> {
        val members = groupKey.split(",").toSet()
        return masterOrder.filter { it in members }
    }

    /**
     * Creates a fresh, default [GroupRotationState] for a previously-unseen group.
     */
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
