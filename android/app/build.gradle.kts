plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    // NOTE: No google-services plugin needed — Supabase doesn't use google-services.json
}

android {
    namespace = "com.roomease.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.roomease.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Supabase credentials are injected at build time from local.properties
        val localProperties = java.util.Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        val supabaseUrl = localProperties.getProperty("SUPABASE_URL") ?: project.findProperty("SUPABASE_URL") ?: ""
        val supabaseKey = localProperties.getProperty("SUPABASE_ANON_KEY") ?: project.findProperty("SUPABASE_ANON_KEY") ?: ""

        buildConfigField("String", "SUPABASE_URL",  "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true   // needed for BuildConfig.SUPABASE_URL
    }
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)

    // Ktor HTTP engine (required by Supabase SDK)
    implementation(libs.ktor.client.okhttp)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Serialization (for @Serializable data models)
    implementation(libs.kotlinx.serialization.json)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
}
