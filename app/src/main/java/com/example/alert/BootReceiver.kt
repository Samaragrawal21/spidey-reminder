package com.example.alert

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.SpideyDatabase
import com.example.data.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = SpideyDatabase.getInstance(appContext).taskDao()
                val activeTasks = dao.getActiveTasksSnapshot()
                val nowMillis = System.currentTimeMillis()
                var missedCount = 0

                for (task in activeTasks) {
                    if (task.scheduledTimeMillis < nowMillis) {
                        dao.updateTask(task.copy(status = TaskEntity.STATUS_MISSED))
                        missedCount++
                    } else {
                        AlarmScheduler.scheduleExactTaskAlarm(appContext, task)
                    }
                }

                if (missedCount > 0) {
                    postSilentMissedSummaryNotification(appContext, missedCount)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postSilentMissedSummaryNotification(context: Context, missedCount: Int) {
        AlertQueueManager.ensureNotificationChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            AlertQueueManager.MISSED_SUMMARY_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summaryMessage = "You missed $missedCount reminders while device was inactive."

        val notification = NotificationCompat.Builder(
            context,
            AlertQueueManager.MISSED_SUMMARY_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(summaryMessage)
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(AlertQueueManager.MISSED_SUMMARY_NOTIFICATION_ID, notification)
    }
}
