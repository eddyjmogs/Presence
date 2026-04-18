# CLAUDE.md — Presence Android App

Read this file at the start of every session. It defines the project, conventions, and current build state.

---

## What This App Is

**Presence** is a personal Android productivity app with two modes:

- **Deep Work Mode** — mandatory check-in overlays on a user-defined interval. Forces accountability via a full-screen overlay that cannot be bypassed.
- **Focus Mode** — lightweight context sessions (commute, bathroom, custom) with per-context app whitelisting and soft reminders.

Full spec is in [docs/context.md](docs/context.md).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM — ViewModel + StateFlow |
| Database | Room (with DAOs and entities, no raw SQL) |
| Background | ForegroundService + BroadcastReceiver |
| Scheduling | AlarmManager (exact alarms for precise intervals) |
| Alarm/Vibration | RingtoneManager, Vibrator |
| Flashlight | CameraManager — `setTorchMode(cameraId, true/false)` |
| Overlay enforcement | AccessibilityService |
| Overlay display | Activity with `SYSTEM_ALERT_WINDOW` + `TYPE_APPLICATION_OVERLAY` |
| Min SDK | API 26 (Android 8.0) |
| Target SDK | Latest stable |

---

## Project Structure

```
app/src/main/
├── java/com/presence/
│   ├── ui/
│   │   ├── home/               # HomeScreen composable + HomeViewModel
│   │   ├── overlay/            # DeepWorkOverlayActivity + OverlayViewModel
│   │   ├── reminder/           # FocusSoftReminderActivity
│   │   ├── history/            # HistoryScreen + HistoryViewModel
│   │   └── settings/           # SettingsScreen, WhitelistManager, PermissionsScreen
│   ├── service/
│   │   ├── PresenceForegroundService.kt
│   │   └── PresenceAccessibilityService.kt
│   ├── receiver/
│   │   └── ScreenStateReceiver.kt      # ACTION_SCREEN_ON / ACTION_SCREEN_OFF
│   ├── alarm/
│   │   └── AlarmReceiver.kt            # AlarmManager broadcast handler
│   ├── data/
│   │   ├── db/                         # Room database, DAOs
│   │   ├── model/                      # Entity data classes
│   │   └── repository/                 # Repository classes (one per domain)
│   ├── state/
│   │   └── SessionStateStore.kt        # SharedPreferences-backed session state
│   └── MainActivity.kt
└── res/
    └── ...
```

---

## Coding Conventions

- **Kotlin only** — no Java files.
- **Compose for all UI** — no XML layouts except the overlay Activity window config.
- **MVVM strictly** — ViewModels expose `StateFlow<UiState>`. Composables collect state and emit events upward. No business logic in composables.
- **Repository pattern** — all data access goes through a repository. ViewModels never touch DAOs directly.
- **Room entities** — use `@Entity`, `@Dao`, `@Database` annotations. No raw SQL strings outside DAOs.
- **No comments explaining what code does** — only add a comment when the *why* is non-obvious (a subtle invariant, a platform workaround, a hidden constraint).
- **No premature abstractions** — implement exactly what the current build step requires. Don't design for hypothetical future features.
- **No error handling for impossible cases** — trust internal guarantees. Only validate at system boundaries (user input, AlarmManager broadcast).
- **StateFlow for session state** — the ForegroundService and AccessibilityService read from `SessionStateStore`, not from the database directly, for low-latency decisions.

---

## Key Implementation Rules

### Overlay (Deep Work)
- The overlay **must** be an `Activity` with window type `TYPE_APPLICATION_OVERLAY`, launched via an Intent with `FLAG_ACTIVITY_NEW_TASK`.
- The Confirm button must remain **disabled** until both required text fields are non-empty.
- `CameraManager.setTorchMode(false)` must always be called when the overlay is confirmed, even if flashlight was not the selected notification type.
- The overlay must re-launch if the user navigates away — enforced by `PresenceAccessibilityService`.

### AccessibilityService
- Handles **both** modes in one service.
- Priority: if Deep Work overlay is pending acknowledgement, always re-launch it regardless of which app is in the foreground.
- If Focus Mode is active (and no Deep Work overlay is pending), check the foreground app package name against the whitelist for the current context.

### AlarmManager
- Use `setExactAndAllowWhileIdle()` for intervals. `setExact()` is not sufficient for Doze mode.
- Always cancel any existing pending alarm before scheduling a new one (use the same request code).

### ForegroundService
- One service handles both modes. It must display a persistent notification while either mode is active.
- Do not stop the service when the app is closed — it must stay alive in the background.

### BroadcastReceiver (Screen State)
- `ScreenStateReceiver` must be registered dynamically in the ForegroundService, not statically in the manifest — `ACTION_SCREEN_ON` / `ACTION_SCREEN_OFF` cannot be received by static receivers.
- On `ACTION_SCREEN_OFF`: save `System.currentTimeMillis()` to `SessionStateStore`.
- On `ACTION_SCREEN_ON`: compute delta, compare against current interval, decide overlay scenario.

### Room Database
- All database writes happen on a background dispatcher (`Dispatchers.IO`).
- ViewModels never write to the database directly — always via repository.

---

## Session State Store

`SessionStateStore` is a SharedPreferences-backed singleton that persists across process restarts. It holds:

| Key | Type | Purpose |
|---|---|---|
| `deepWorkActive` | Boolean | Whether a Deep Work session is running |
| `focusModeActive` | Boolean | Whether Focus Mode is running |
| `focusModeContext` | String | Current Focus Mode context label |
| `currentIntervalMinutes` | Int | The user-set interval for the current check-in |
| `timerStartTime` | Long | Epoch millis when the current interval started |
| `timerExpired` | Boolean | Whether AlarmManager has fired and overlay is pending |
| `screenOffTime` | Long | Epoch millis of last screen-off event |
| `notificationTypes` | String (JSON) | Alarm/Vibration/Flashlight/Silent flags |

---

## Build Order & Current Step

| Step | Feature | Status |
|---|---|---|
| 1 | Home screen UI — task input, mode buttons, today's log | ✓ |
| 2 | Deep Work: ForegroundService skeleton + persistent notification | ✓ |
| 3 | Deep Work: Mandatory overlay Activity (all fields) | ✓ |
| 4 | Deep Work: AlarmManager scheduling from overlay confirm | ✓ |
| 5 | Deep Work: Alarm / vibration / flashlight / silent on timer fire | ✓ |
| 6 | Deep Work: Screen-on/off BroadcastReceiver + away time detection | ✓ |
| 7 | Deep Work: Overlay heading logic (on time / over time / away) | ✓ |
| 8 | Deep Work: Room database for log entries | ✓ |
| 9 | Deep Work: AccessibilityService overlay enforcement | ✓ |
| 10 | Focus Mode: Whitelist manager UI (installed apps checklist) | ✓ |
| 11 | Focus Mode: AccessibilityService extension for whitelist checking | ✓ |
| 12 | Focus Mode: Soft reminder screen | ✓ |
| 13 | Focus Mode: Custom context creation | ✓ |
| 14 | History screen (combined log, grouped by day) | ✓ |
| 15 | Settings screen | ✓ |
| 16 | Polish: onboarding, permission prompts, edge cases, battery optimization | ✓ |

**Update this table as steps are completed.** Mark done steps with ✓.

---

## Permissions

| Permission | Declared in Manifest | Runtime Prompt |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | Yes | Manual — Settings redirect |
| `POST_NOTIFICATIONS` | Yes | Yes (Android 13+) |
| `BIND_ACCESSIBILITY_SERVICE` | Yes | Manual — Accessibility Settings redirect |
| `SCHEDULE_EXACT_ALARM` | Yes | Yes / Settings redirect (Android 12+) |
| `USE_FULL_SCREEN_INTENT` | Yes | Settings redirect (Android 14+) |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Yes | Runtime prompt |
| `CAMERA` | Yes | Not needed — torch uses CameraManager directly without CAMERA permission on most devices; test on target device |
| `VIBRATE` | Yes | No prompt required |
| `FOREGROUND_SERVICE` | Yes | No prompt required |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Yes (if needed) | No prompt required |

---

## Critical Rules

1. **Never break existing functionality when adding a new feature.** Each build step must leave the app in a working state.
2. **Test the overlay on a real device** — emulators do not reliably simulate `SYSTEM_ALERT_WINDOW`, `AccessibilityService`, or AlarmManager Doze behavior.
3. **The overlay is the most important feature** — if it can be bypassed, the app fails its core purpose. Always verify overlay enforcement after any AccessibilityService change.
4. **AlarmManager exact alarms require the permission to be granted** — always check `alarmManager.canScheduleExactAlarms()` before scheduling and redirect to settings if false.
5. **Torch must always be turned off on overlay confirm** — a stuck flashlight is a bad user experience and a bug.
6. **Don't touch the manifest without checking all permission flags** — incorrect `foregroundServiceType` declarations cause crashes on Android 14+.
