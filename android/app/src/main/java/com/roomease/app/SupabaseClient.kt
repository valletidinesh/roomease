package com.roomease.app

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Singleton Supabase client.
 *
 * URL and anon key are injected from local.properties at build time via BuildConfig.
 * They are NEVER hardcoded in source files.
 *
 * Access from anywhere: SupabaseClient.client
 */
object SupabaseClient {

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Postgrest)
            install(Auth)
            install(Realtime)
        }
    }
}
