package com.roomease.app

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "roomease_prefs")

class RoomEaseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Touch the Supabase client early so it's ready on first use
        SupabaseClient.client
    }
}

object PrefsKeys {
    val ROOM_ID  = stringPreferencesKey("room_id")
    val USER_UID = stringPreferencesKey("user_uid")
}

suspend fun Context.getSavedRoomId(): String? =
    dataStore.data.map { it[PrefsKeys.ROOM_ID] }.first()

suspend fun Context.saveRoomId(roomId: String) {
    dataStore.edit { it[PrefsKeys.ROOM_ID] = roomId }
}

suspend fun Context.getSavedUserUid(): String? =
    dataStore.data.map { it[PrefsKeys.USER_UID] }.first()

suspend fun Context.saveUserUid(uid: String) {
    dataStore.edit { it[PrefsKeys.USER_UID] = uid }
}
