# RoomEase — Android App

A task manager for roommates. Built with **Kotlin + Jetpack Compose + Firebase Firestore**.

## Project layout
```
android/               ← Open THIS folder in Android Studio
  app/
    src/main/java/com/roomease/app/
      data/model/      ← Data classes (User, GroupRotationState, etc.)
      data/repository/ ← Firestore reads/writes
      domain/          ← Pure business logic (RotationEngine, TrashSelector…)
      ui/screens/      ← Compose screens
      ui/theme/        ← Colors, Typography, Theme
      ui/navigation/   ← Screen routes
      MainActivity.kt
      RoomEaseApp.kt
firestore.rules        ← Deploy these to Firebase Console
```

## First-time setup (do this once)

### 1. Create Firebase project
1. Go to https://console.firebase.google.com → Create project → name: `roomease`
2. **Firestore** → Create database → Production mode → Region: `asia-south1`
3. **Authentication** → Sign-in method → **Email/Password** → Enable

### 2. Register the Android app
1. Firebase Console → Project settings → Add app → Android
2. Package name: `com.roomease.app`
3. Download `google-services.json`
4. Place it at: `android/app/google-services.json`

### 3. Deploy Firestore security rules
1. Open Firebase Console → Firestore → Rules tab
2. Paste the contents of `firestore.rules`
3. Click Publish

### 4. Open in Android Studio
1. Open Android Studio → Open → select the `android/` folder
2. Wait for Gradle sync to complete
3. Click ▶ Run (or Shift+F10)

## How it works

| Layer | Location | Purpose |
|---|---|---|
| Domain logic | `domain/` | Rotation engine, trash selector, water pairs — pure Kotlin, no Firebase |
| Data layer | `data/repository/` | All Firestore reads/writes, real-time listeners as `Flow<T>` |
| UI | `ui/screens/` | Jetpack Compose screens |
| State | Firestore real-time | All 6 phones stay in sync automatically |

## Onboarding flow
```
Install APK → Sign up with email → Create Room
  → Enter room name
  → Add roommates (name + optional email)
  → Set rotation order (A → B → C → D)
  → Assign washroom groups (W1 / W2)
  → Share 6-digit invite code with roommates
```

## Building the APK for sharing
```
Build → Generate Signed Bundle / APK → APK → Create keystore → Build
```
Upload `app-release.apk` to Google Drive, share link in WhatsApp.
Each phone: tap link → download → Settings → Install unknown apps → Install.

## Architecture decisions
- **Firestore transactions** on all `markDone()` calls → no race conditions
- **Domain layer is pure Kotlin** → full JUnit unit test coverage possible
- **11 group states pre-seeded** on room creation → no cold start delay
- **3-cycle cooking history pruning** runs client-side after each write
