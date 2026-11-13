# ATIK AI AGENT (Android 15 • Kotlin & Jetpack Compose)

**Developer:** ATIK  
**Target Device:** Redmi Turbo 3 (Android 15 / HyperOS China ROM)

## Architecture Highlights
- **Official Android APIs Only:** Uses `NotificationListenerService` + `RemoteInput` + `PendingIntent`. Zero Accessibility abuse, zero Device Admin, zero hidden APIs.
- **Graceful App Compatibility:** Not every third-party app or notification type (e.g., imo calls, media previews, reactions) exposes an inline `RemoteInput` action. Unavailable reply actions are detected and logged cleanly as `No RemoteInput Action` without workarounds.
- **Privacy & Safety Floor:**
  - `SensitiveContentFilter` blocks OTPs, passwords, PINs, card numbers, and bKash/Nagad/Rocket/bank alerts **locally on-device** before any Gemini call.
  - `DeduplicationGuard` prevents duplicate replies on notification updates and blocks self-echo reply loops with per-conversation cooldowns and hourly caps.
  - **Manual Approval Mode is the default.**
  - Room DB + DataStore stay strictly local (`android:allowBackup="false"`, excluded from cloud backup/device transfer).
- **Firebase AI Logic + App Check:** Uses `com.google.firebase:firebase-ai` (`GenerativeBackend.googleAI()`) protected by `PlayIntegrityAppCheckProviderFactory` (release) and `DebugAppCheckProviderFactory` (debug).

## Build Instructions
1. Open the `android-project/` directory in **Android Studio Ladybug / Meerkat** (JDK 17).
2. Add your `app/google-services.json` from Firebase Console (see `FIREBASE_SETUP.md`).
3. Run unit tests: `./gradlew testDebugUnitTest`
4. Install debug build on Redmi Turbo 3: `./gradlew installDebug`
