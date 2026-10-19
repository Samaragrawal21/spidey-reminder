package com.example.alert

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SpideyDatabase
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.SpideyCrimson
import com.example.ui.theme.SpideyReminderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SpideyLockAlertActivity : ComponentActivity() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        configureLockScreenAndWakeUp()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Prevent accidental back button dismissal without choosing Done or Snooze
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Require explicit Done or Snooze action on alarm hijack screen
                }
            }
        )

        val fallbackTaskId = intent?.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L) ?: -1L

        setContent {
            SpideyReminderTheme {
                val activeTask by AlertQueueManager.currentAlertTask.collectAsStateWithLifecycle()
                val queuedCount by AlertQueueManager.queuedCount.collectAsStateWithLifecycle()

                // If launched directly from notification tap when singleton state was cleared, reload from DB
                LaunchedEffect(activeTask, fallbackTaskId) {
                    if (activeTask == null && fallbackTaskId > 0L) {
                        val loaded = withContext(Dispatchers.IO) {
                            SpideyDatabase.getInstance(applicationContext).taskDao().getTaskById(fallbackTaskId)
                        }
                        if (loaded != null) {
                            AlertQueueManager.enqueueTaskAlert(applicationContext, loaded)
                        } else {
                            finishAndReleaseWakeLock()
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MatteBlack)
                        .safeDrawingPadding(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Subtle Comic Web Backdrop
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, 0f)
                        val maxRadius = size.height * 0.85f
                        val webColor = SpideyCrimson.copy(alpha = 0.12f)
                        for (i in 1..6) {
                            drawCircle(
                                color = webColor,
                                radius = (maxRadius / 6f) * i,
                                center = center,
                                style = Stroke(width = 2f)
                            )
                        }
                    }

                    val task = activeTask
                    if (task != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            SpideyAlertCardContent(
                                task = task,
                                queuedCount = queuedCount,
                                onDoneClick = {
                                    AlertQueueManager.markCurrentTaskDone(applicationContext) {
                                        finishAndReleaseWakeLock()
                                    }
                                },
                                onSnoozeMinutes = { mins ->
                                    AlertQueueManager.snoozeCurrentTask(applicationContext, mins) {
                                        finishAndReleaseWakeLock()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun configureLockScreenAndWakeUp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "SpideyReminder:LockScreenAlertWakeLock"
        )
        wl.setReferenceCounted(false)
        wl.acquire(60_000L) // Short 60-second WakeLock to light up display
        wakeLock = wl
    }

    private fun finishAndReleaseWakeLock() {
        AlertQueueManager.cancelLockNotification(applicationContext)
        releaseWakeLockSafely()
        if (!isFinishing) {
            finish()
        }
    }

    private fun releaseWakeLockSafely() {
        val wl = wakeLock
        wakeLock = null
        if (wl != null && wl.isHeld) {
            try {
                wl.release()
            } catch (ignored: Exception) {
            }
        }
    }

    override fun onDestroy() {
        releaseWakeLockSafely()
        super.onDestroy()
    }
}
