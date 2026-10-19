package com.example.alert

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.SpideyDatabase
import com.example.data.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.ArrayDeque

object AlertQueueManager {

    const val LOCK_ALARM_CHANNEL_ID = "spidey_lock_alarm_channel"
    const val OVERLAY_SERVICE_CHANNEL_ID = "spidey_overlay_service_channel"
    const val MISSED_SUMMARY_CHANNEL_ID = "spidey_missed_summary_channel"

    const val LOCK_NOTIFICATION_ID = 9001
    const val OVERLAY_NOTIFICATION_ID = 9002
    const val MISSED_SUMMARY_NOTIFICATION_ID = 9003

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pendingQueue = ArrayDeque<TaskEntity>()
    private val queueLock = Any()

    private val _currentAlertTask = MutableStateFlow<TaskEntity?>(null)
    val currentAlertTask: StateFlow<TaskEntity?> = _currentAlertTask.asStateFlow()

    private val _queuedCount = MutableStateFlow(0)
    val queuedCount: StateFlow<Int> = _queuedCount.asStateFlow()

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val lockChannel = NotificationChannel(
                LOCK_ALARM_CHANNEL_ID,
                "Spidey Full-Screen Lock Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority full-screen reminders when device is locked or screen is off"
                setSound(null, null) // Audio is handled directly by SymbioteSoundPlayer via AudioAssetManager
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val overlayChannel = NotificationChannel(
                OVERLAY_SERVICE_CHANNEL_ID,
                "Spidey Floating Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground special-use service channel for active floating reminder overlays"
                setSound(null, null)
            }

            val missedChannel = NotificationChannel(
                MISSED_SUMMARY_CHANNEL_ID,
                "Spidey Missed Reminders Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Silent summary notification after device reboot"
                setSound(null, null)
                enableVibration(false)
            }

            manager.createNotificationChannel(lockChannel)
            manager.createNotificationChannel(overlayChannel)
            manager.createNotificationChannel(missedChannel)
        }
    }

    fun enqueueTaskAlert(context: Context, task: TaskEntity) {
        val appContext = context.applicationContext
        ensureNotificationChannels(appContext)

        var shouldPresentImmediately = false
        synchronized(queueLock) {
            val active = _currentAlertTask.value
            if (active == null) {
                _currentAlertTask.value = task
                _queuedCount.value = pendingQueue.size
                shouldPresentImmediately = true
            } else if (active.id != task.id && pendingQueue.none { it.id == task.id }) {
                pendingQueue.addLast(task)
                _queuedCount.value = pendingQueue.size
            }
        }

        if (shouldPresentImmediately) {
            presentActiveTask(appContext, task)
        }
    }

    private fun presentActiveTask(context: Context, task: TaskEntity) {
        // Start alert audio from AudioAssetManager on AudioAttributes.USAGE_ALARM
        SymbioteSoundPlayer.start(context)

        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        val isLocked = keyguardManager.isKeyguardLocked
        val isInteractive = powerManager.isInteractive
        val canDrawOverlay = Settings.canDrawOverlays(context)

        // Scenario A: Screen Off or Device Locked -> Full Screen Intent targeting SpideyLockAlertActivity
        // Scenario B: Device Unlocked & In-Use -> ReminderService floating WindowManager overlay
        // (If overlay permission is not granted yet, gracefully use Scenario A + Activity launch so alert is never lost)
        if (isLocked || !isInteractive || !canDrawOverlay) {
            fireFullScreenLockAlert(context, task)
        } else {
            val serviceIntent = Intent(context, ReminderService::class.java).apply {
                action = ReminderService.ACTION_SHOW_OVERLAY
                putExtra(AlarmScheduler.EXTRA_TASK_ID, task.id)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    private fun fireFullScreenLockAlert(context: Context, task: TaskEntity) {
        val fullScreenIntent = Intent(context, SpideyLockAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmScheduler.EXTRA_TASK_ID, task.id)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, LOCK_ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("SPIDEY REMINDER")
            .setContentText(task.title)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(LOCK_NOTIFICATION_ID, notification)

        try {
            context.startActivity(fullScreenIntent)
        } catch (ignored: Exception) {
        }
    }

    fun markCurrentTaskDone(context: Context, onQueueEmpty: () -> Unit = {}) {
        val appContext = context.applicationContext
        // Immediately stop and release MediaPlayer
        SymbioteSoundPlayer.stopAndRelease()

        val taskToComplete = synchronized(queueLock) { _currentAlertTask.value } ?: run {
            onQueueEmpty()
            return
        }

        scope.launch(Dispatchers.IO) {
            val dao = SpideyDatabase.getInstance(appContext).taskDao()
            val updated = taskToComplete.copy(status = TaskEntity.STATUS_COMPLETED)
            dao.updateTask(updated)
            AlarmScheduler.cancelTaskAlarm(appContext, taskToComplete.id)

            advanceQueueOnMain(appContext, onQueueEmpty)
        }
    }

    fun snoozeCurrentTask(context: Context, minutes: Int, onQueueEmpty: () -> Unit = {}) {
        val appContext = context.applicationContext
        val safeMinutes = minutes.coerceAtLeast(1)
        // Immediately stop and release MediaPlayer
        SymbioteSoundPlayer.stopAndRelease()

        val taskToSnooze = synchronized(queueLock) { _currentAlertTask.value } ?: run {
            onQueueEmpty()
            return
        }

        scope.launch(Dispatchers.IO) {
            val dao = SpideyDatabase.getInstance(appContext).taskDao()
            val nextTimeMillis = System.currentTimeMillis() + safeMinutes * 60_000L
            val updated = taskToSnooze.copy(
                status = TaskEntity.STATUS_SNOOZED,
                scheduledTimeMillis = nextTimeMillis
            )
            dao.updateTask(updated)
            AlarmScheduler.scheduleExactTaskAlarm(appContext, updated)

            advanceQueueOnMain(appContext, onQueueEmpty)
        }
    }

    private fun advanceQueueOnMain(context: Context, onQueueEmpty: () -> Unit) {
        scope.launch(Dispatchers.Main.immediate) {
            val nextTask: TaskEntity? = synchronized(queueLock) {
                val polled = if (pendingQueue.isNotEmpty()) pendingQueue.removeFirst() else null
                _currentAlertTask.value = polled
                _queuedCount.value = pendingQueue.size
                polled
            }

            if (nextTask != null) {
                // Present next queued task immediately and restart symbiote audio
                SymbioteSoundPlayer.start(context)
            } else {
                cancelLockNotification(context)
                stopOverlayService(context)
                onQueueEmpty()
            }
        }
    }

    fun cancelLockNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(LOCK_NOTIFICATION_ID)
    }

    private fun stopOverlayService(context: Context) {
        val serviceIntent = Intent(context, ReminderService::class.java).apply {
            action = ReminderService.ACTION_DISMISS_OVERLAY
        }
        try {
            context.stopService(serviceIntent)
        } catch (ignored: Exception) {
        }
    }
}
