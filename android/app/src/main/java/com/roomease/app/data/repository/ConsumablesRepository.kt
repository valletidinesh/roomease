package com.roomease.app.data.repository

import com.roomease.app.SupabaseClient
import com.roomease.app.data.model.BuyListItem
import com.roomease.app.data.model.PurchaseEntry
import com.roomease.app.data.model.UsageLog
import com.roomease.app.domain.ConsumableSplit
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
// ConsumablesRepository
// ─────────────────────────────────────────────────────────────────────────────
class ConsumablesRepository {
    private val db get() = SupabaseClient.client

    fun listenToOpenEntries(roomId: String): Flow<List<PurchaseEntry>> = flow {
        val channel = db.realtime.channel("consumables-$roomId")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "purchase_entries"
            filter(FilterOperation("room_id", FilterOperator.EQ, roomId))
        }
        channel.subscribe()
        emit(getOpenEntries(roomId))
        changes.collect { emit(getOpenEntries(roomId)) }
    }

    suspend fun getOpenEntries(roomId: String): List<PurchaseEntry> {
        return db.from("purchase_entries")
            .select {
                filter { eq("room_id", roomId); eq("status", "OPEN") }
                order("bought_at", Order.DESCENDING)
            }
            .decodeList()
    }

    suspend fun createEntry(
        roomId: String,
        item: String,
        totalQty: Int,
        totalPrice: Double,
        boughtBy: String,
    ) {
        db.from("purchase_entries").insert(
            PurchaseEntry(
                roomId = roomId,
                item = item,
                totalQty = totalQty,
                totalPrice = totalPrice,
                boughtBy = boughtBy,
                status = "OPEN",
                finalSplit = emptyMap()
            )
        )
    }

    suspend fun logUsage(roomId: String, entryId: String, userId: String, qty: Int) {
        // Validate remaining qty
        val logs = getUsageLogs(roomId, entryId)
        val entry = db.from("purchase_entries")
            .select { filter { eq("id", entryId) } }
            .decodeSingle<PurchaseEntry>()

        val remaining = ConsumableSplit.remainingQty(entry, logs)
        require(qty <= remaining) { "Quantity exceeds remaining ($remaining left)" }
        check(entry.status == "OPEN") { "Cannot log usage on a closed entry" }

        db.from("usage_logs").insert(
            UsageLog(
                roomId = roomId,
                purchaseEntryId = entryId,
                userId = userId,
                qty = qty
            )
        )
    }

    suspend fun getUsageLogs(roomId: String, entryId: String): List<UsageLog> {
        return db.from("usage_logs")
            .select { filter { eq("room_id", roomId); eq("purchase_entry_id", entryId) } }
            .decodeList()
    }

    suspend fun closeEntry(roomId: String, entryId: String) {
        val entry = db.from("purchase_entries")
            .select { filter { eq("id", entryId) } }
            .decodeSingle<PurchaseEntry>()
        val logs = getUsageLogs(roomId, entryId)
        val split = ConsumableSplit.calculate(entry, logs)

        db.from("purchase_entries").update(
            {
                set("status", "CLOSED")
                set("final_split", split.amountsOwed)
            }
        ) { filter { eq("id", entryId); eq("room_id", roomId) } }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BuyListRepository
// ─────────────────────────────────────────────────────────────────────────────
class BuyListRepository {
    private val db get() = SupabaseClient.client

    fun listenToBuyList(roomId: String): Flow<List<BuyListItem>> = flow {
        val channel = db.realtime.channel("buylist-$roomId")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "buy_list"
            filter(FilterOperation("room_id", FilterOperator.EQ, roomId))
        }
        channel.subscribe()
        emit(getBuyList(roomId))
        changes.collect { emit(getBuyList(roomId)) }
    }

    suspend fun getBuyList(roomId: String): List<BuyListItem> {
        return db.from("buy_list")
            .select {
                filter { eq("room_id", roomId) }
                order("added_at", Order.DESCENDING)
            }
            .decodeList()
    }

    suspend fun addItem(roomId: String, itemName: String, addedBy: String) {
        db.from("buy_list").insert(
            BuyListItem(
                roomId = roomId,
                itemName = itemName,
                addedBy = addedBy,
                status = com.roomease.app.data.model.BuyStatus.PENDING
            )
        )
    }

    suspend fun markBought(roomId: String, itemId: String, boughtBy: String) {
        db.from("buy_list").update(
            mapOf("status" to "BOUGHT", "bought_by" to boughtBy)
        ) { filter { eq("id", itemId); eq("room_id", roomId) } }
    }

    suspend fun deleteItem(roomId: String, itemId: String) {
        db.from("buy_list").delete {
            filter { eq("id", itemId); eq("room_id", roomId) }
        }
    }
}
