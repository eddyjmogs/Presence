# Presence

A personal Android productivity app with two complementary modes for staying intentional with your time.

---

## What It Does

### Deep Work Mode

Start a work session with a task name. Presence fires an alarm, vibration, or flashlight (your choice) when your set interval expires. When you pick up your phone, a **mandatory full-screen overlay** appears — you must log what you worked on and set your next focus before you can do anything else. No skipping, no dismissing.

Each check-in records:
- What you worked on
- What you're focusing on next
- How long until the next check-in
- How you want to be notified (alarm, vibration, flashlight, or silent)
- Optional notes

While a session is active, a banner on the home screen shows the current task and a Stop button.

### Focus Mode

Enable it before a commute, bathroom break, or any context where you want to be intentional. Select a context (Commute, Bathroom, or any custom context you've created), and only your whitelisted apps for that context are freely accessible. Opening anything else shows a soft reminder — *"Focus mode is on. You're commuting."* — with a Go Back / Continue Anyway choice. No hard block, just a conscious pause.

---

## Screens

| Screen | Description |
|---|---|
| **Home** | Task input, Start Deep Work, Enable Focus Mode dropdown, today's log, All History link |
| **Deep Work Overlay** | Mandatory check-in — fills what you did, next focus, interval, notification type |
| **Focus Soft Reminder** | Non-blocking prompt when a non-whitelisted app is opened |
| **Whitelist Manager** | Per-context installed-apps checklist |
| **History** | All log entries grouped by day |
| **Settings** | Contexts, Deep Work defaults, permissions status |

---

## Two Modes, One Log

Everything is recorded in a combined daily log visible from the home screen and the History screen:

```
9:15am  — Deep Work · 30 min | Did: "Read chapter intro" | Next: "Take notes"
9:47am  — Deep Work · 45 min | Did: "Notes done" | Next: "Practice problems"
8:30am  — Deep Work · 20 min | Did: "Reviewed notes" | Next: "Section 2"
```

---

## Build & Run

**Requirements:**
- Android Studio Iguana or later
- JDK 21 (bundled with Android Studio)
- Android device or emulator running API 26+ (Android 8.0+)

**From Android Studio:** Open the project and sync Gradle, then run.

**From the command line:**
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Required Permissions

Grant these on first launch (the app walks you through them) or via **Settings → Permissions** in the app.

| Permission | Why | How to Grant |
|---|---|---|
| **Draw over other apps** | Mandatory Deep Work overlay | Settings → Apps → Presence → Display over other apps |
| **Accessibility Service** | Overlay enforcement + Focus Mode whitelist detection | Settings → Accessibility → Presence |
| **Notifications** | Persistent session notification | Prompted at runtime |
| **Schedule exact alarms** | Precise interval timers | Settings → Apps → Presence → Alarms & Reminders (Android 12+) |
| **Battery optimization** | Keep background service alive through Doze | Prompted during onboarding |
| **Full-screen intents** | Show overlay on lock screen | Declared in manifest; prompted if needed (Android 14+) |

---

## Distribution

Built for personal use and sharing with a small group — no Play Store required.

### Option 1: Direct APK

```bash
./gradlew assembleRelease
# Share app/build/outputs/apk/release/app-release.apk
```

Recipients enable **Install from unknown sources** in their Android settings, then open the APK.

### Option 2: Firebase App Distribution

```bash
npm install -g firebase-tools
firebase login
firebase appdistribution:distribute app/build/outputs/apk/release/app-release.apk \
  --app <your-firebase-app-id> \
  --groups "friends"
```

Testers receive an email with an install link and get notified on new releases. Recommended for ongoing distribution.

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM — ViewModel + StateFlow
- **Database:** Room 2.7.1 (entities: `LogEntry`, `WhitelistEntry`, `FocusContext`)
- **Background:** ForegroundService + dynamically registered BroadcastReceiver
- **Scheduling:** AlarmManager (`setExactAndAllowWhileIdle`)
- **Notifications:** Alarm via `RingtoneManager`, vibration via `Vibrator`, flashlight via `CameraManager`
- **Overlay enforcement:** AccessibilityService
- **Session state:** SharedPreferences (`SessionStateStore`)
- **Min SDK:** API 26 (Android 8.0)
- **Target SDK:** 36
