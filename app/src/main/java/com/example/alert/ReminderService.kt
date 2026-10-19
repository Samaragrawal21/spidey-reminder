package com.example.alert

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.R
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.SpideyReminderTheme

class ReminderService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.example.alert.ACTION_SHOW_OVERLAY"
        const val ACTION_DISMISS_OVERLAY = "com.example.alert.ACTION_DISMISS_OVERLAY"
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AlertQueueManager.ensureNotificationChannels(this)
        startSpecialUseForeground()

        when (intent?.action) {
            ACTION_DISMISS_OVERLAY -> {
                removeOverlayView()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                if (Settings.canDrawOverlays(this)) {
                    showOrUpdateOverlay()
                } else {
                    // Fallback to full-screen lock activity if overlay permission was revoked
                    val currentTask = AlertQueueManager.currentAlertTask.value
                    if (currentTask != null) {
                        val lockIntent = Intent(this, SpideyLockAlertActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra(AlarmScheduler.EXTRA_TASK_ID, currentTask.id)
                        }
                        startActivity(lockIntent)
                    }
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startSpecialUseForeground() {
        val notification = NotificationCompat.Builder(this, AlertQueueManager.OVERLAY_SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Active Spider-Sense Task Reminder Overlay")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

        ServiceCompat.startForeground(
            this,
            AlertQueueManager.OVERLAY_NOTIFICATION_ID,
            notification,
            fgsType
        )
    }

    private fun showOrUpdateOverlay() {
        if (overlayView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Allow focus so keyboard input works for the Custom Snooze field
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ReminderService)
            setViewTreeViewModelStoreOwner(this@ReminderService)
            setViewTreeSavedStateRegistryOwner(this@ReminderService)

            setContent {
                SpideyReminderTheme {
                    val activeTask by AlertQueueManager.currentAlertTask.collectAsStateWithLifecycle()
                    val queuedCount by AlertQueueManager.queuedCount.collectAsStateWithLifecycle()

                    val task = activeTask
                    if (task != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MatteBlack.copy(alpha = 0.92f))
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            SpideyAlertCardContent(
                                task = task,
                                queuedCount = queuedCount,
                                onDoneClick = {
                                    AlertQueueManager.markCurrentTaskDone(applicationContext) {
                                        removeOverlayView()
                                        stopSelf()
                                    }
                                },
                                onSnoozeMinutes = { mins ->
                                    AlertQueueManager.snoozeCurrentTask(applicationContext, mins) {
                                        removeOverlayView()
                                        stopSelf()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        try {
            windowManager?.addView(composeView, params)
            overlayView = composeView
        } catch (ignored: Exception) {
        }
    }

    private fun removeOverlayView() {
        val view = overlayView
        overlayView = null
        if (view != null) {
            try {
                windowManager?.removeView(view)
            } catch (ignored: Exception) {
            }
        }
    }

    override fun onDestroy() {
        removeOverlayView()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
