package com.roomease.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * Mirrors the `users` table in Supabase.
 * Column names use snake_case via @SerialName — matches PostgreSQL convention.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class User(
    val uid: String = "",
    @SerialName("room_id")   val roomId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    /** 0-based position in master rotation order */
    @EncodeDefault @SerialName("master_order")      val masterOrder: Int = 0,
    @EncodeDefault val status: String = "ACTIVE",   // UserStatus enum stored as string
    @EncodeDefault val presence: String = "PRESENT",
    @EncodeDefault @SerialName("washroom_group")    val washroomGroup: Int = 1,
    @EncodeDefault @SerialName("water_fetch_count") val waterFetchCount: Int = 0,
    @SerialName("last_fetched_at")   val lastFetchedAt: String? = null,
    @EncodeDefault @SerialName("trash_wet_count")   val trashWetCount: Int = 0,
    @EncodeDefault @SerialName("trash_dry_count")   val trashDryCount: Int = 0,
    @SerialName("last_trash_at")     val lastTrashAt: String? = null,
    @SerialName("joined_at")         val joinedAt: String? = null,
)

// ── Convenience helpers ───────────────────────────────────────────────────────

val User.isActive: Boolean   get() = status == "ACTIVE"
val User.isPresent: Boolean  get() = presence == "PRESENT"
val User.isEligible: Boolean get() = isActive && isPresent
val User.completeTurns: Int  get() = minOf(trashWetCount, trashDryCount)
