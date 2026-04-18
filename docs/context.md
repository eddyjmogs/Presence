# Presence — Full Technical Context

## App Concept

Presence is a personal Android productivity app with two complementary modes:

- **Deep Work Mode** — flexible user-defined check-in intervals with mandatory accountability overlays
- **Focus Mode** — lightweight context-based sessions (commute, bathroom, custom) with app whitelisting and soft reminders

One app, two modes, shared infrastructure. Built for personal use and distribution to a small group via APK or Firebase App Distribution.

---

## Mode 1: Deep Work Mode

### User Flow

1. User opens app, types or selects their current task (e.g. "Study math")
2. Taps **Start Deep Work** → leaves the app
3. When the user-defined timer fires, a notification/alarm/vibration triggers (based on choice)
4. When the user picks up their phone, a **mandatory full-screen overlay** appears
5. User completes the overlay, sets the next interval, and confirms
6. Repeat until session is stopped

### The Overlay — Three Scenarios

**Scenario A: On time or early** (phone picked up within the set interval)
- Heading: none / neutral
- Required: "What did you work on?"
- Required: "What's your focus for the next interval?"

**Scenario B: Over time** (phone picked up after interval expired)
- Heading: *"You set [X] minutes — it's been [Y] minutes."*
- Required: "What happened / what did you work on?"
- Required: "What's your focus for the next interval?"

**Scenario C: Returning after long idle** (screen off ≥ set interval, no active timer)
- Heading: *"You were away for [X] hours/minutes."*
- Required: "What did you do during that time?"
- Required: "What's your focus for the next interval?"

### Overlay Confirmation Area (always shown at bottom)

```
┌─────────────────────────────────┐
│ What's your focus?              │
│ [________________________]      │
│                                 │
│ Next check-in in: [30] minutes  │
│                                 │
│ Notify me via (pick any):        │
│ [x] Alarm  [ ] Vibration        │
│ [ ] Flashlight  [ ] Silent      │
│                                 │
│ Notes: [____________________]   │
│                  (optional)     │
│                                 │
│         [ Confirm ]             │
└─────────────────────────────────┘
```

- **Next check-in duration** — user sets the next interval (15, 30, 90 min, etc.). No enforced default.
- **Notify me** — per-interval, any combination via checkboxes:
  - **Alarm** — audible alarm when timer fires
  - **Vibration** — silent vibration only
  - **Flashlight** — torch turns on when timer fires, stays on until overlay is acknowledged
  - **Silent** — no interruption; overlay appears naturally on next phone pickup
- **Notes** — optional free text (ideas, blockers, reminders)
- Confirm button is disabled until required fields are filled

### Key Rules
- Overlay is **mandatory** — user cannot dismiss or navigate around it without completing required fields
- Required fields: what you worked on + next focus
- Notes and interval settings are always visible; notes are optional
- If the user goes over their interval, heading reflects this contextually
- Silent mode means no interruption; overlay appears on next screen-on

### Deep Work Log Format

```
9:00am  — Started session: "Study math"
9:15am  — Did: "Read chapter intro" | Next: "Take notes on section 1" | 30min | Alarm
9:47am  — Did: "Notes done (took longer)" | Next: "Practice problems" | 45min | Vibration
          Notes: "Section 1 was dense, need to reread"
10:35am — Away 48min: "Made coffee, took a walk" | Next: "Review notes" | 20min | Silent
```

---

## Mode 2: Focus Mode

### Concept

A lightweight mode for captive time (commute, bathroom, custom context). The user enables it, picks a context, and only whitelisted apps are freely accessible. Everything else shows a soft reminder.

### User Flow

1. On home screen, tap **Enable Focus Mode**
2. Select context: Commute / Bathroom / [custom label]
3. Focus Mode activates — persistent notification shows: *"Focus Mode: Commute 🚇"*
4. User uses phone freely within whitelisted apps
5. Opening a non-whitelisted app → soft reminder screen appears
6. User manually taps **End Focus Mode** → optionally logs what they did

### Soft Reminder Screen

When a non-whitelisted app is opened during Focus Mode:
- *"Focus mode is on. You're commuting."*
- *"[Current intention if set]"*
- Two buttons: **Go Back** | **Continue Anyway**
- No hard block — user can always proceed but must consciously choose to

### Whitelisting

Each Focus Mode context has its own whitelist, configured in Settings via an installed-apps checklist.

Example:
- 🚇 Commute: Kindle, Spotify, Anki, Notes
- 🚽 Bathroom: Anki, Duolingo
- Custom contexts: user-defined name + custom whitelist

### Focus Mode Log Format

```
8:30am — Commute (45 min) | Did: "Read 20 pages of Kindle"
1:00pm — Bathroom (15 min) | Did: "Anki flashcards"
6:00pm — Commute (45 min) | Did: "Listened to podcast"
```

---

## Home Screen Layout

```
┌─────────────────────────────────┐
│         PRESENCE                │
│                                 │
│  Current task: "Study math"     │
│                                 │
│  [ Start Deep Work Session ]    │
│                                 │
│  [ Enable Focus Mode ▾ ]        │
│    → Commute                    │
│    → Bathroom                   │
│    → + Custom...                │
│                                 │
│  ── Today's Log ──              │
│  9:15am  Did: "Read chapter 2"  │
│  9:47am  Did: "Practice probs"  │
│  ...                            │
└─────────────────────────────────┘
```

---

## All Screens

### 1. Home Screen
- Active task input
- Start Deep Work button
- Enable Focus Mode dropdown (Commute / Bathroom / Custom)
- Today's combined log (both modes)

### 2. Deep Work Overlay (mandatory)
- Drawn over all other apps via `SYSTEM_ALERT_WINDOW`
- Cannot be dismissed before completing required fields
- Context-aware heading (on time / over time / away)
- Required fields: what you did, next focus
- Optional: notes
- Always shown: next interval duration + notification type selector

### 3. Focus Mode Soft Reminder (non-mandatory)
- Appears when non-whitelisted app is opened during Focus Mode
- Shows current context and intention
- Go Back / Continue Anyway buttons

### 4. History Screen
- Combined log of all sessions (both modes) grouped by day
- Shows mode, context, duration, interval settings, notes

### 5. Settings
- **Whitelist Manager** — per Focus Mode context, installed apps checklist
- **Custom Contexts** — create, rename, delete Focus Mode contexts
- **Deep Work Settings** — default interval suggestion, default notification type
- **Permissions** — status and shortcuts to grant required permissions

---

## Technical Architecture

### Shared Components

| Component | Purpose |
|---|---|
| `AccessibilityService` | Core of both modes. Deep Work: re-surfaces overlay if user navigates away. Focus Mode: detects foreground app, checks whitelist, shows soft reminder |
| `ForegroundService` | Keeps both modes alive in background. Shows persistent notification for active mode |
| `Room` database | Stores log entries, whitelist configs, custom contexts, interval settings, session history |
| `BroadcastReceiver` | Listens for `ACTION_SCREEN_ON` / `ACTION_SCREEN_OFF` to track screen-off duration |

### Deep Work Mode Specific

| Component | Purpose |
|---|---|
| `SYSTEM_ALERT_WINDOW` overlay | Mandatory full-screen check-in UI drawn over all apps |
| `AlarmManager` | Schedules user-defined interval timer. Fires alarm/vibration/flashlight/silent based on user choice |
| `RingtoneManager` / `Vibrator` | Plays alarm sound or vibration when timer fires |
| `CameraManager` | Controls torch — `setTorchMode(true)` when timer fires, `setTorchMode(false)` when overlay is acknowledged |
| Screen-off tracker | Records timestamp on screen off, computes away duration on screen on |
| Interval state store | Persists current interval duration, notification type, and timer start time across process restarts |

### Focus Mode Specific

| Component | Purpose |
|---|---|
| Whitelist store (Room) | Stores allowed app package names per context |
| Installed apps query | `PackageManager` to list all installed apps for whitelist setup UI |
| Foreground app detection | Via `AccessibilityService` — checks current app against whitelist |
| Soft reminder Activity | Simple activity shown over non-whitelisted apps |

---

## AccessibilityService Logic

```
On foreground app change:
  if Deep Work Mode active AND overlay pending acknowledgement:
    → re-launch mandatory overlay
  else if Focus Mode active:
    → check if new app is in whitelist for current context
    → if NOT in whitelist: launch soft reminder screen
    → if in whitelist: do nothing
```

---

## Timer & Notification Logic (Deep Work)

```
User confirms overlay:
  → save interval duration + notification type to state
  → schedule AlarmManager for (now + interval)
  → set "pending acknowledgement" flag = false

When AlarmManager fires:
  → if notification includes Alarm: play alarm sound via RingtoneManager
  → if notification includes Vibration: vibrate via Vibrator
  → if notification includes Flashlight: CameraManager.setTorchMode(true)
  → if Silent only: do nothing
  → set "timer expired" flag = true

When overlay is acknowledged (user taps Confirm):
  → CameraManager.setTorchMode(false) — always called to ensure torch is off
  → save log entry to Room
  → schedule next AlarmManager if continuing session
```

---

## Screen-off Detection Logic

```
ACTION_SCREEN_OFF received:
  → record timestamp to SharedPreferences/state store

ACTION_SCREEN_ON received:
  → compute delta = now - recorded timestamp
  → if Deep Work session active:
      if "timer expired" flag = true:
        → show overlay with Scenario B heading (over time)
      else if delta >= interval:
        → show overlay with Scenario C heading (away)
      else:
        → do nothing (user is within interval)
  → if Deep Work session inactive AND delta is notable:
      → optionally show Scenario C for idle tracking
```

---

## Permissions Required

| Permission | Purpose | How Granted |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw overlay over other apps | Manual grant in Android Settings |
| `POST_NOTIFICATIONS` | Persistent status notifications | Runtime prompt (Android 13+) |
| `AccessibilityService` | Detect foreground app changes | Manual grant in Accessibility Settings |
| `SCHEDULE_EXACT_ALARM` | Precise interval timers | Runtime prompt or Settings (Android 12+) |
| `USE_FULL_SCREEN_INTENT` | Full-screen overlay on lock screen | Declared in manifest; may need grant (Android 14+) |
| Battery optimization disable | Keep ForegroundService alive | Prompt user on first launch |

All permissions requested on first launch with plain-language explanations.

---

## Suggested Build Order

| Step | Feature |
|---|---|
| 1 | Home screen UI — task input, mode buttons, today's log |
| 2 | Deep Work: ForegroundService skeleton + persistent notification |
| 3 | Deep Work: Mandatory overlay Activity with all fields (what you did, next focus, interval picker, notification type, notes) |
| 4 | Deep Work: AlarmManager scheduling from overlay confirm |
| 5 | Deep Work: Alarm / vibration / flashlight / silent notification on timer fire |
| 6 | Deep Work: Screen-on/off BroadcastReceiver + away time detection |
| 7 | Deep Work: Overlay heading logic (on time / over time / away) |
| 8 | Deep Work: Room database for log entries |
| 9 | Deep Work: AccessibilityService to enforce overlay (re-launch if navigated away) |
| 10 | Focus Mode: Whitelist manager UI (installed apps checklist per context) |
| 11 | Focus Mode: AccessibilityService extension for whitelist checking |
| 12 | Focus Mode: Soft reminder screen |
| 13 | Focus Mode: Custom context creation |
| 14 | History screen (combined log, grouped by day) |
| 15 | Settings screen |
| 16 | Polish: onboarding, permission prompts, edge cases, battery optimization |

---

## Known Issues & Recommended Next Steps

These are confirmed gaps in the current implementation, in priority order.

### Critical (functional holes)

**1. Missing manifest permission for battery optimization**
`android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is not declared in `AndroidManifest.xml`. The battery optimization exemption request silently fails without it. One-line fix.

**2. No boot receiver — alarms lost on device restart**
`AlarmManager` alarms are wiped when the device restarts. If a Deep Work session is active and the phone reboots, the timer is silently lost. Needs a `RECEIVE_BOOT_COMPLETED` receiver that reads `SessionStateStore` on boot and reschedules the alarm if `deepWorkActive && timerStartTime > 0`.

**3. OnTime overlay doesn't fire when screen is already on**
When the alarm fires and the screen is already on, the overlay only surfaces via `AccessibilityService` detecting the next app switch. If the user stays in the same app after the alarm fires, no overlay appears until they switch apps. `AlarmReceiver` should directly launch the overlay for the OnTime scenario when the alarm fires (check `PowerManager.isScreenOn()`).

**4. Focus Mode sessions are never logged**
There is no `LogEntry` written when a Focus Mode session ends. The history screen will be empty for all Focus Mode usage. Needs a log entry write in `PresenceForegroundService` when `ACTION_STOP_FOCUS_MODE` is processed (duration = now − `timerStartTime`, mode = "FOCUS_MODE").

### Important (UX gaps)

**5. Stop button has no confirmation**
Tapping Stop during an active Deep Work session discards the current interval with no warning. A simple confirmation dialog ("End session? Your current interval won't be logged.") prevents accidental taps.

**6. No notification Stop action**
The persistent notification has no Stop action button. Users expect to be able to stop a session from the notification shade without opening the app. Needs a `PendingIntent` action on the notification that sends `ACTION_STOP` / `ACTION_STOP_FOCUS_MODE` directly to the service.

**7. Activity writes session state before the service does**
In `MainActivity.launchDeepWork()` and `startFocusMode()`, the activity pre-writes to `SessionStateStore` to make the session banner appear immediately. The service will write the same values again when it processes the intent. This is harmless today but creates two sources of truth. The correct pattern is to only write from the service and refresh the UI via a small delay or lifecycle event.

---

## Future Ideas

- Weekly summary: "8 out of 10 commutes were productive this week"
- Per-context stats and average interval lengths
- Harder block option for Focus Mode
- Shared accountability — send daily log summary to a friend
- Suggested interval based on task type or time of day
