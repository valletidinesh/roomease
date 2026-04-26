# Supabase (keep models accessible for serialization)
-keep class com.roomease.app.data.model.** { *; }
-keepclassmembers class com.roomease.app.data.model.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class ** { @kotlinx.serialization.Serializable *; }
-keep,includedescriptorclasses class com.roomease.app.**$$serializer { *; }
-keepclassmembers class com.roomease.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.roomease.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor (HTTP client used by Supabase SDK)
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
