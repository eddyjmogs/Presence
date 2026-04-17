# Presence

A personal Android productivity app with two complementary modes for staying intentional with your time.

---

## What It Does

### Deep Work Mode

Start a work session with a task name. Presence fires an alarm/vibration/flashlight (your choice) when your set interval expires. When you pick up your phone, a **mandatory full-screen overlay** appears — you must log what you worked on and set your next focus before you can do anything else. No skipping, no dismissing.

Each check-in records:
- What you worked on
- What you're focusing on next
- How long until the next check-in
- How you want to be notified (alarm, vibration, flashlight, or silent)
- Optional notes

### Focus Mode

Enable it before a commute, bathroom break, or any context where you want to be intentional. Only your whitelisted apps are freely accessible. Opening anything else shows a soft reminder — *"Focus mode is on. You're commuting."* — with a Go Back / Continue Anyway choice. No hard block, just a conscious pause.

---

## Two Modes, One Log

Everything is recorded in a combined daily log:

```
9:00am  — Started session: "Study math"
9:15am  — Did: "Read chapter intro" | Next: "Take notes" | 30min | Alarm
9:47am  — Did: "Notes done" | Next: "Practice problems" | 45min | Vibration
8:30am  — Commute (45 min) | Did: "Read 20 pages of Kindle"
```

---

## Build & Run

**Requirements:**
- Android Studio Hedgehog or later
- JDK 17
- Android device or emulator running API 26+ (Android 8.0+)

**Steps:**

```bash
git clone <repo-url>
cd Presence
# Open in Android Studio and sync Gradle
# Or build from CLI:
./gradlew assembleDebug
```

Install on device:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Required Permissions

Grant these on first launch or via the Settings screen in the app.

| Permission | Why | How to Grant |
|---|---|---|
| **Draw over other apps** (`SYSTEM_ALERT_WINDOW`) | Mandatory Deep Work overlay | Settings → Apps → Presence → Display over other apps |
| **Notifications** | Persistent mode notification | Prompted at runtime |
| **Accessibility Service** | Detect foreground app for overlay enforcement and Focus Mode whitelist | Settings → Accessibility → Presence |
| **Schedule exact alarms** | Precise interval timers | Settings → Apps → Presence → Alarms & Reminders (Android 12+) |
| **Full-screen intents** | Show overlay on lock screen | Declared in manifest; prompted if needed (Android 14+) |
| **Battery optimization** | Keep background service alive | Settings → Battery → Unrestricted for Presence |

The app walks you through all of these on first launch with plain-language explanations.

---

## Distribution

This app is built for personal use and sharing with a small group — no Play Store required.

### Option 1: Direct APK

1. Build a release APK:
   ```bash
   ./gradlew assembleRelease
   ```
2. Share `app/build/outputs/apk/release/app-release.apk` via Google Drive, email, or AirDrop equivalent
3. Recipients enable **Install from unknown sources** in their Android settings, then open the APK

### Option 2: Firebase App Distribution

1. Set up a Firebase project and add the app
2. Install the Firebase CLI and log in:
   ```bash
   npm install -g firebase-tools
   firebase login
   ```
3. Distribute:
   ```bash
   firebase appdistribution:distribute app/build/outputs/apk/release/app-release.apk \
     --app <your-firebase-app-id> \
     --groups "friends"
   ```
4. Testers receive an email with an install link and get notified on new releases

Firebase App Distribution is the recommended option — it handles versioning, install prompts, and update notifications automatically.

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** MVVM with ViewModel + StateFlow
- **Database:** Room
- **Background:** ForegroundService + BroadcastReceiver
- **Scheduling:** AlarmManager
- **Notifications:** Alarm, Vibration via `Vibrator`, Flashlight via `CameraManager`
- **Overlay enforcement:** AccessibilityService
- **Min SDK:** API 26 (Android 8.0)
