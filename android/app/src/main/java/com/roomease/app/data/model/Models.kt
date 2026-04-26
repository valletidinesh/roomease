package com.roomease.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `group_rotation_state` table */
@Serializable
data class GroupRotationState(
    val id: String = "",
    @SerialName("room_id")               val roomId: String = "",
    @SerialName("group_key")             val groupKey: String = "",
    /** Stored as a JSON array in Supabase, decoded by the SDK */
    val sequence: List<String> = emptyList(),
    @SerialName("current_cycle_order")   val currentCycleOrder: List<String> = emptyList(),
    @SerialName("cycle_index")           val cycleIndex: Int = 0,
    @SerialName("last_actual_user_id")   val lastActualUserId: String? = null,
    @SerialName("current_cycle_num")     val currentCycleNum: Int = 1,
    @SerialName("updated_at")            val updatedAt: String? = null,
)

/** `cooking_history` table */
@Serializable
data class CookingHistory(
    val id: String = "",
    @SerialName("room_id")           val roomId: String = "",
    @SerialName("group_key")         val groupKey: String = "",
    @SerialName("assigned_user_id")  val assignedUserId: String = "",
    @SerialName("actual_user_id")    val actualUserId: String = "",
    @SerialName("cooked_at")         val cookedAt: String? = null,
    @SerialName("cycle_number")      val cycleNumber: Int = 1,
) {
    val isOverride: Boolean get() = assignedUserId != actualUserId
}

/** `trash_history` table */
@Serializable
data class TrashHistory(
    val id: String = "",
    @SerialName("room_id")                val roomId: String = "",
    @SerialName("user_id")                val userId: String = "",
    @SerialName("trash_type")             val trashType: String = "WET",
    @SerialName("thrown_at")              val thrownAt: String? = null,
    @SerialName("complete_turns_after")   val completeTurnsAfter: Int = 0,
)

enum class TrashType { WET, DRY }

/** `washroom_state` table */
@Serializable
data class WashroomState(
    val id: String = "",
    @SerialName("room_id")          val roomId: String = "",
    @SerialName("washroom_number")  val washroomNumber: Int = 1,
    @SerialName("group_order")      val groupOrder: List<String> = emptyList(),
    @SerialName("cycle_index")      val cycleIndex: Int = 0,
    val status: String = "ACTIVE",   // "ACTIVE" | "PENDING_RESUME"
)

/** `water_history` table */
@Serializable
data class WaterHistory(
    val id: String = "",
    @SerialName("room_id")    val roomId: String = "",
    val pair: List<String> = emptyList(),
    @SerialName("fetched_at") val fetchedAt: String? = null,
)

/** `purchase_entries` table */
@Serializable
data class PurchaseEntry(
    val id: String = "",
    @SerialName("room_id")     val roomId: String = "",
    val item: String = "",
    @SerialName("total_qty")   val totalQty: Int = 0,
    @SerialName("total_price") val totalPrice: Double = 0.0,
    @SerialName("bought_by")   val boughtBy: String = "",
    @SerialName("bought_at")   val boughtAt: String? = null,
    val status: String = "OPEN",  // "OPEN" | "CLOSED"
    @SerialName("final_split") val finalSplit: Map<String, Double> = emptyMap(),
)

/** `usage_logs` table */
@Serializable
data class UsageLog(
    val id: String = "",
    @SerialName("room_id")            val roomId: String = "",
    @SerialName("purchase_entry_id")  val purchaseEntryId: String = "",
    @SerialName("user_id")            val userId: String = "",
    val qty: Int = 0,
    @SerialName("logged_at")          val loggedAt: String? = null,
)

/** `buy_list` table */
@Serializable
data class BuyListItem(
    val id: String = "",
    @SerialName("room_id")    val roomId: String = "",
    @SerialName("item_name")  val itemName: String = "",
    @SerialName("added_by")   val addedBy: String = "",
    @SerialName("added_at")   val addedAt: String? = null,
    val status: String = "PENDING",  // "PENDING" | "BOUGHT"
    @SerialName("bought_by")  val boughtBy: String? = null,
    @SerialName("bought_at")  val boughtAt: String? = null,
)

/** `rooms` table */
@Serializable
data class Room(
    val id: String = "",
    val name: String = "",
    @SerialName("admin_uid")       val adminUid: String = "",
    @SerialName("invite_code")     val inviteCode: String = "",
    @SerialName("master_order")    val masterOrder: List<String> = emptyList(),
    @SerialName("washroom_groups") val washroomGroups: Map<String, List<String>> = emptyMap(),
    @SerialName("created_at")      val createdAt: String? = null,
)
