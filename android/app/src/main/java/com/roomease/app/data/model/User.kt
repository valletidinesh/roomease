package com.roomease.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors the `users` table in Supabase.
 * Column names use snake_case via @SerialName — matches PostgreSQL convention.
 */
@Serializable
data class User(
    val uid: String = "",
    @SerialName("room_id")   val roomId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    /** 0-based position in master rotation order */
    @SerialName("master_order")      val masterOrder: Int = 0,
    val status: String = "ACTIVE",   // UserStatus enum stored as string
    val presence: String = "PRESENT",
    @SerialName("washroom_group")    val washroomGroup: Int = 1,
    @SerialName("water_fetch_count") val waterFetchCount: Int = 0,
    @SerialName("last_fetched_at")   val lastFetchedAt: String? = null,
    @SerialName("trash_wet_count")   val trashWetCount: Int = 0,
    @SerialName("trash_dry_count")   val trashDryCount: Int = 0,
    @SerialName("last_trash_at")     val lastTrashAt: String? = null,
    @SerialName("joined_at")         val joinedAt: String? = null,
)

// ── Convenience helpers ───────────────────────────────────────────────────────

val User.isActive: Boolean   get() = status == "ACTIVE"
val User.isPresent: Boolean  get() = presence == "PRESENT"
val User.isEligible: Boolean get() = isActive && isPresent
val User.completeTurns: Int  get() = minOf(trashWetCount, trashDryCount)
