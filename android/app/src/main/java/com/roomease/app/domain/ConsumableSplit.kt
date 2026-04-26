package com.roomease.app.domain

import com.roomease.app.data.model.PurchaseEntry
import com.roomease.app.data.model.UsageLog

/**
 * Computes cost split for a purchase entry.
 * Formula: (userQty / totalUsed) * totalPrice
 *
 * This is a pure computation — no side effects, fully testable.
 */
object ConsumableSplit {

    data class SplitResult(
        /** Maps userId → amount this user owes for this batch. */
        val amountsOwed: Map<String, Double>,
        /** How much the buyer is owed by others (totalPrice - buyerShare). */
        val buyerOwed: Double,
        val totalUsed: Int,
    )

    /**
     * Calculates the split for all usage logs against [entry].
     * Users who logged no usage owe ₹0.
     */
    fun calculate(entry: PurchaseEntry, logs: List<UsageLog>): SplitResult {
        val totalUsed = logs.sumOf { it.qty }
        if (totalUsed == 0) {
            return SplitResult(emptyMap(), entry.totalPrice, 0)
        }

        val amountsOwed = logs
            .groupBy { it.userId }
            .mapValues { (_, userLogs) ->
                val userQty = userLogs.sumOf { it.qty }
                (userQty.toDouble() / totalUsed) * entry.totalPrice
            }

        val buyerShare = amountsOwed[entry.boughtBy] ?: 0.0
        val buyerOwed = entry.totalPrice - buyerShare

        return SplitResult(
            amountsOwed = amountsOwed,
            buyerOwed = buyerOwed,
            totalUsed = totalUsed,
        )
    }

    /**
     * Validates that new usage doesn't exceed the batch total.
     * Returns the remaining quantity available.
     */
    fun remainingQty(entry: PurchaseEntry, existingLogs: List<UsageLog>): Int {
        val used = existingLogs.sumOf { it.qty }
        return (entry.totalQty - used).coerceAtLeast(0)
    }
}
