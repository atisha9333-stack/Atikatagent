# Firebase AI Logic & App Check Setup Guide — ATIK AI AGENT

**Developer:** ATIK  
**Target Device:** Redmi Turbo 3 (Android 15, HyperOS China ROM)  
**Package Name:** `com.atik.aiagent` (debug suffix: `com.atik.aiagent.debug`)

---

## 1. Create & Configure Firebase Project
1. Open [Firebase Console](https://console.firebase.google.com/) and create a project (e.g., `atik-ai-agent`).
2. Add an Android app with package name **`com.atik.aiagent`** (and optionally add **`com.atik.aiagent.debug`** if you want App Check & AI Logic to work on debug builds).
3. Download **`google-services.json`** and place it inside:
   ```
   android-project/app/google-services.json
   ```
   *(Remove `.template` or keep `google-services.json` alongside it; the Gradle Google Services plugin reads `app/google-services.json`.)*

## 2. Enable Firebase AI Logic (Gemini)
1. In Firebase Console, open **Build → AI Logic** (formerly Vertex AI in Firebase).
2. Click **Get Started** and select **Gemini Developer API** (matches `GenerativeBackend.googleAI()` in `GeminiReplyGenerator.kt`).
3. Ensure the **Generative Language API** is enabled on the linked Google Cloud project.
4. No Gemini API key needs to be hardcoded in Kotlin—Firebase AI Logic proxies requests securely through Firebase with App Check verification.

## 3. Enable Firebase App Check (Important for China ROM!)
1. In Firebase Console, go to **Build → App Check → Apps**.
2. Register `com.atik.aiagent` with **Play Integrity** (for release builds installed via Play Store or certified GMS).
   - Provide your release keystore **SHA-256** fingerprint.
3. **For Local USB Debug Builds on Redmi Turbo 3 (China ROM):**
   - `AtikAiAgentApp.kt` automatically installs `DebugAppCheckProviderFactory` when `BuildConfig.DEBUG` is true.
   - Run the debug app on your Redmi Turbo 3 via USB (`adb logcat | grep DebugAppCheckProvider`).
   - Copy the printed **Debug Secret UUID** and register it under **App Check → Apps → ⋮ → Manage debug tokens** in Firebase Console.
4. **Redmi Turbo 3 China ROM GMS Note:**
   - On HyperOS China ROM, enable **Settings → Additional settings → Accounts & sync → Google Basic Services** so Play Integrity and Firebase Play Services can communicate cleanly.

## 4. Test Order (Required Safety Workflow)
1. **Stage 1 (No AI yet):** Grant Notification Access (after unlocking *Allow restricted settings* in App Info `⋮`), open the **Test** tab in ATIK AI AGENT, and run:
   - `Test RemoteInput` (verifies `NotificationListenerService` + `RemoteInput` round-trip).
   - `Test No-Reply` (verifies graceful handling when `RemoteInput` is absent).
   - `Sensitive Content Shield` (verifies OTP, password, bKash/Nagad blocking).
2. **Stage 2 (Manual Approval Default):** Enable Messenger / imo detection while keeping **Manual Approval Mode** ON. Confirm natural Bangla & English drafts appear in the Dashboard queue.
3. **Stage 3 (Optional Auto-Reply):** Only after verifying Stage 1 & 2, switch to **Auto-Reply** and configure allowlists, cooldowns, and group-chat ignore rules.
