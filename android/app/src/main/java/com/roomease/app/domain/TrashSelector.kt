package com.roomease.app.domain

import com.roomease.app.data.model.TrashType
import com.roomease.app.data.model.User
import com.roomease.app.data.model.completeTurns
import com.roomease.app.data.model.isEligible

/**
 * Trash selection: load-balanced, no fixed rotation.
 * Sort priority:
 *   1. Fewest complete turns (min(wet, dry)) — primary fairness metric
 *   2. Fewest throws of this specific type — secondary balance
 *   3. Who went longest ago — final tiebreaker
 */
object TrashSelector {

    /**
     * Returns the eligible user who should throw next.
     * @throws IllegalStateException if no eligible users are present.
     */
    fun getNextThrower(users: List<User>, trashType: TrashType): User {
        val eligible = users.filter { it.isEligible }
        check(eligible.isNotEmpty()) { "No eligible users present" }

        return eligible.sortedWith(
            compareBy(
                { it.completeTurns },
                { if (trashType == TrashType.WET) it.trashWetCount else it.trashDryCount },
                { it.lastTrashAt ?: "" },
            )
        ).first()
    }
}
