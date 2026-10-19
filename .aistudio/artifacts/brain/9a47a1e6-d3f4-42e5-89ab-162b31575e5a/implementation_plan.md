# Spidey Reminder

**Spidey Reminder** is a 100% offline, custom task reminder engine for Android 15 (API 35) built with Kotlin and Jetpack Compose. It pairs a bold Dark Spider-Man Comic aesthetic with a dual-mode alert hijack engine that commands attention whether the device is locked or actively in use, backed by a sequential task alert queue and local symbiote theme audio playback.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product and UX decisions were confirmed during interactive clarification and govern the implementation alongside the core specification:

- **Target Date & Rollover Logic (Confirmed)**: Manual 12-hour (`HH:MM` + `AM/PM`) keyboard time entry automatically schedules the reminder for **today** if the entered time is still in the future, or rolls over to **tomorrow's occurrence** if the entered time has already passed today.
- **Android 15 Permission Setup & Alert Testing (Confirmed)**: The Home screen includes an interactive **Permission Checklist & Diagnostic Banner** for Android 15 runtime and special permissions (`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, and `SYSTEM_ALERT_WINDOW` Display Over Other Apps) along with a one-tap **Test Alert** button so users can immediately verify the full-screen/overlay hijack and symbiote theme audio without waiting for a scheduled clock boundary.
- **Active Task Card Quick Actions (Confirmed)**: In addition to the **Edit** action on each Active Task card, cards provide direct **Mark Done Early** (transitions task to `COMPLETED` and cancels its alarm) and **Delete Task** (cancels its alarm and deletes the single record) controls.

---

## 1. Overview & Core Concept

- **What It Does**: Allows users to create, edit, track, and respond to custom time-critical task reminders completely offline. When a scheduled alarm triggers, the app inspects device state (`KeyguardManager` and `PowerManager`) to either wake and hijack the lock screen or deploy a floating `WindowManager` overlay with the hanging Spider-Man graphic (`spiderman.png`) and symbiote alarm audio (`black_suit_theme.mp3`).
- **Target Audience / Persona**: Users who want a high-impact, zero-distraction, comic-styled reminder utility where alerts cannot be accidentally overlooked or buried in silent notification shade trays.
- **Key Value**: Reliable exact-time scheduling, automatic multi-task alert queueing (preventing overlapping alert chaos), instant fixed/custom snooze controls, and post-reboot missed-task recovery.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Home & Permission Readiness Flow**:
   - On launch, the user lands on **Active Tasks** featuring the upside-down hanging Spider-Man illustration (`spiderman.png`) descending from the top bar and a comic-styled header.
   - If notifications, exact alarms, or overlay permissions are not yet granted, a compact Crimson-accented status banner lets the user grant each permission with one tap or fire a **Test Alert** to preview the alarm experience.
2. **Task Creation & Editing Flow**:
   - Tapping the Crimson Red **+ Floating Action Button** (or the **Edit** icon on an existing card) opens the **Task Creator & Editor Dialog**.
   - The user types an alphanumeric task title and enters the target Hour (`01`–`12`) and Minute (`00`–`59`) via outlined numeric keyboard inputs, toggles **[AM | PM]** via a segmented selector, sees the computed target schedule preview (Today vs. Tomorrow), and taps **Save Task**.
3. **Active Task Management Flow**:
   - Active and snoozed tasks appear in chronological order (`scheduledTimeMillis ASC`) with 12-hour formatted time (`08:30 AM`), status pill (`PENDING` or `SNOOZED`), live relative countdown badge (e.g., `in 2h 14m`), and quick actions for **Done**, **Edit**, and **Delete**.
4. **Dual-Mode Alert Hijack & Queue Flow**:
   - When one or more tasks reach their scheduled time, the **Task Queueing Singleton** enqueues them sequentially.
   - **Locked / Screen Off**: Launches a Full Screen Intent opening the dedicated lock-screen alert screen (`setShowWhenLocked(true)`, `setTurnScreenOn(true)`, plus a WakeLock) and starts looping the symbiote theme over `USAGE_ALARM`.
   - **Unlocked & Interactive**: Starts the Foreground Special-Use Reminder Service and inflates an interactive floating `WindowManager` overlay (`TYPE_APPLICATION_OVERLAY`) directly over the current screen.
   - The user can tap **Done / Complete**, pick a fixed snooze chip (**+15m**, **+30m**, **+1h**, **+2h**), or type a **Custom Snooze** minute value and confirm. Audio stops immediately, the database updates, and if another task is waiting in the queue, it is presented right away.
5. **History & Tracking Flow**:
   - Switching to the **History** tab displays a chronological log of resolved past tasks (`COMPLETED` and `MISSED` only; `SNOOZED` tasks remain active on the Active Tasks screen until resolved) filtered by top filter chips: **Last 1 Day**, **Last 7 Days**, **Last 30 Days**, and **Last 6 Months**.
   - Each card displays a color-coded status badge and border (**Green** for `COMPLETED`, **Red** for `MISSED`) with single-item swipe-to-delete or trash icon removal (no bulk delete).

### Visual Identity & Theme
- **Aesthetic Direction**: Dark Spider-Man Comic UI — sharp geometric contrast, subtle comic-web radial linework in the header backdrop, and high-visibility crimson borders.
- **Color Palette & Mood**:
  - **Primary Background**: Matte Black (`#111111`)
  - **Surface Containers**: Dark Slate / Grey (`#1E1E1E`) with elevated card surfaces (`#252525`)
  - **Brand Accents**: Spider-Man Crimson Red (`#E23636`) and Web White (`#FFFFFF`)
  - **Text Hierarchy**: High-contrast Web White (`#FFFFFF`) titles and subdued light-grey metadata (`#AAAAAA`)
  - **Status Semantic Accents**: Completed Emerald (`#2ECC71`), Missed Crimson (`#E23636`), Snoozed Amber-Orange (`#F39C12`)
- **Typography & Hierarchy**:
  - Locally bundled Google Fonts (`Bangers` or `Anton` for bold comic display headers paired with `Space Grotesk` / `Inter` for crisp numeric time badges and task titles) scaled in `sp`.
- **Component Styling & Layout**:
  - Edge-to-edge layout respecting status bar and bottom navigation bar insets.
  - High-contrast task cards with 1.5dp–2dp colored borders, sharp 12dp comic-panel corners, and 48dp minimum touch targets.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Sequential Singleton Alert Queue vs. Parallel Windows**
  - *Chosen Approach*: A thread-safe `AlertQueueManager` singleton holds triggered tasks in FIFO order and drives a single active alert presentation and `MediaPlayer` instance at a time.
  - *Why*: Prevents overlapping `WindowManager` views, duplicate audio streams, and race conditions when two reminders are scheduled for the same minute.
  - *Alternatives Considered*: Spawning separate activities or notifications per task, which causes audio overlap and confusing stacked windows.
- **Decision 2: Dual-Path Presentation (Overlay vs. Full-Screen Lock Activity) with Graceful Fallback**
  - *Chosen Approach*: Inspect `KeyguardManager.isKeyguardLocked` and `PowerManager.isInteractive` when the alarm broadcast arrives. Use `WindowManager` (`TYPE_APPLICATION_OVERLAY`) when unlocked and `Settings.canDrawOverlays(context)` is true; otherwise launch the full-screen intent lock activity backed by a high-priority alarm notification.
  - *Why*: Delivers an authentic floating hijack when the user is actively using their phone while guaranteeing delivery on the lock screen or if overlay permission hasn't been toggled yet.
- **Decision 3: 100% Offline Room Persistence with Strict Single-Item Deletion**
  - *Chosen Approach*: Use Room (`TaskEntity`, `TaskDao`) returning reactive `Flow<List<TaskEntity>>` streams for both Active Tasks and History, omitting any bulk-clear methods.
  - *Why*: Guarantees zero network dependency, instant reactive UI updates, and strict adherence to data auditability requirements.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         UI LAYER (Jetpack Compose)                        │
│                                                                           │
│  ┌─────────────────────────┐  ┌─────────────────────────┐                 │
│  │   Active Tasks Screen   │  │  History & Track Screen │                 │
│  │  • Hanging Spidey Header│  │  • 1D / 7D / 30D / 6M   │                 │
│  │  • Permission & Test Bar│  │    Filter Chips         │                 │
│  │  • Countdown Task Cards │  │  • Status-Tagged Cards  │                 │
│  │  • Creator/Editor Modal │  │  • Single-Item Delete   │                 │
│  └────────────┬────────────┘  └────────────┬────────────┘                 │
│               └─────────────┬──────────────┘                              │
│                             ▼                                             │
│                  ┌─────────────────────┐                                  │
│                  │   TaskViewModel     │                                  │
│                  └──────────┬──────────┘                                  │
└─────────────────────────────┼─────────────────────────────────────────────┘
                              ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                      DOMAIN & DATA REPOSITORY LAYER                       │
│                                                                           │
│  ┌─────────────────────────┐               ┌───────────────────────────┐  │
│  │     TaskRepository      │◄─────────────►│   AlarmSchedulerEngine    │  │
│  │  • CRUD & Status Flows  │               │  • setAlarmClock / Exact  │  │
│  └────────────┬────────────┘               │  • Cancel by task.id      │  │
│               ▼                            └─────────────┬─────────────┘  │
│  ┌─────────────────────────┐                             │                │
│  │  Room Database (SQLite) │                             │                │
│  │  • TaskDao / TaskEntity │                             │                │
│  └────────────▲────────────┘                             ▼                │
└───────────────┼──────────────────────────────────────────┼────────────────┘
                │                                          │
┌───────────────┼──────────────────────────────────────────┼────────────────┐
│               │     SYSTEM & DUAL ALERT ENGINE           │                │
│               │                                          ▼                │
│  ┌────────────┴────────────┐               ┌───────────────────────────┐  │
│  │      BootReceiver       │               │      AlarmReceiver        │  │
│  │  • Marks past PENDING / │               │  • Checks Keyguard & Power│  │
│  │    SNOOZED as MISSED    │               │  • Enqueues Task into     │  │
│  │  • Summary Notification │               │    AlertQueueManager      │  │
│  │  • Reschedules future   │               └─────────────┬─────────────┘  │
│  └─────────────────────────┘                             │                │
│                                                          ▼                │
│                                            ┌───────────────────────────┐  │
│                                            │    AlertQueueManager      │  │
│                                            │  • FIFO Task Queue        │  │
│                                            │  • SymbioteAudioPlayer    │  │
│                                            │    (R.raw.black_suit)     │  │
│                                            └──────┬─────────────┬──────┘  │
│                                  Locked / Off     │             │ Active  │
│                                        ▼          │             │   ▼     │
│                           ┌───────────────────────┘             └──────┐  │
│                           ▼                                            ▼  │
│             ┌───────────────────────────┐        ┌──────────────────────┐ │
│             │  SpideyLockAlertActivity  │        │   ReminderService    │ │
│             │  • FullScreenIntent       │        │  • Foreground Special│ │
│             │  • TurnScreenOn + WakeLock│        │  • WindowManager     │ │
│             │  • Done / +15m..+2h / Cust│        │    Overlay Hijack    │ │
│             └───────────────────────────┘        └──────────────────────┘ │
└───────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State
- **`TaskEntity` (`tasks` table)**:
  - `id: Long` (`@PrimaryKey(autoGenerate = true)`) — also serves as the deterministic `PendingIntent` request code for `AlarmManager`.
  - `title: String` — user-entered task description.
  - `scheduledTimeMillis: Long` — exact epoch timestamp in milliseconds for the next alarm trigger.
  - `status: String` — one of `"PENDING"`, `"COMPLETED"`, `"MISSED"`, `"SNOOZED"`.
  - `createdAtMillis: Long` — creation timestamp used for history window filtering.
- **`TaskDao` Queries**:
  - `getPendingTasks(): Flow<List<TaskEntity>>` — selects tasks where `status IN ('PENDING', 'SNOOZED') ORDER BY scheduledTimeMillis ASC`.
  - `getHistoryTasks(sinceMillis: Long): Flow<List<TaskEntity>>` — selects tasks where `status IN ('COMPLETED', 'MISSED') AND createdAtMillis >= :sinceMillis ORDER BY scheduledTimeMillis DESC` (`SNOOZED` tasks remain active in `getPendingTasks()` until resolved).
  - `getActiveTasksSync(): List<TaskEntity>` & `getTaskByIdSync(id: Long): TaskEntity?` — suspend/synchronous helpers used by `BootReceiver` and `AlarmReceiver`.
  - Single-item mutations: `insertTask(task: TaskEntity): Long`, `updateTask(task: TaskEntity)`, `deleteTaskById(id: Long)`.
- **Android 15 Special-Use Foreground Service Compliance**:
  - `ReminderService` declares `android:foregroundServiceType="specialUse"` alongside the mandatory `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="..." />` explaining the floating task reminder overlay hijack use case.

### Interactive Component & State Mapping
- **Task Creator & Editor Modal**:
  - Validates non-empty title, numeric hour (`1..12`), and numeric minute (`0..59`).
  - Converts 12-hour (`HH:MM` + `AM/PM`) into a `Calendar` instance for today; if `calendar.timeInMillis <= System.currentTimeMillis()`, adds 1 day so it schedules for tomorrow.
  - Persists via `TaskRepository`, schedules exact alarm via `AlarmScheduler`, and closes modal.
- **Dual Alert Actions (Shared across Lock Screen Activity & WindowManager Overlay)**:
  - **Done / Complete**: Immediately stops and releases `MediaPlayer` (`R.raw.black_suit_theme`), updates task status in Room to `"COMPLETED"`, cancels any pending alarm intent for `task.id`, and pops the next queued task in `AlertQueueManager` (or closes the overlay/activity if queue is empty).
  - **Fixed Snooze Chips (`+15m`, `+30m`, `+1h`, `+2h`)**: Stops audio, computes `newTime = System.currentTimeMillis() + offsetMillis`, updates status to `"SNOOZED"` and `scheduledTimeMillis = newTime`, schedules a new exact alarm, and advances the queue.
  - **Custom Snooze Input**: Validates positive minute integer (`1..1440`), applies the custom offset identically to fixed snooze chips, and advances the queue.
- **Boot Recovery (`BootReceiver` on `ACTION_BOOT_COMPLETED`)**:
  - Runs asynchronously with `goAsync()`. Queries all `"PENDING"` and `"SNOOZED"` tasks.
  - Tasks with `scheduledTimeMillis < now` are updated to `"MISSED"` and counted; if `missedCount > 0`, posts a silent summary notification: `"You missed [X] reminders while device was inactive."`
  - Tasks with `scheduledTimeMillis >= now` have their exact alarms re-registered with `AlarmManager`.
