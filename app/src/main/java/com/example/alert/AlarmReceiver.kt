package com.example.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.SpideyDatabase
import com.example.data.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_TASK_ALARM = "com.example.alert.ACTION_TRIGGER_TASK_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val taskId = intent?.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L) ?: -1L
        val fallbackTitle = intent?.getStringExtra(AlarmScheduler.EXTRA_TASK_TITLE) ?: "Spidey Task Reminder"
        if (taskId <= 0L) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = SpideyDatabase.getInstance(appContext).taskDao()
                val task = dao.getTaskById(taskId) ?: TaskEntity(
                    id = taskId,
                    title = fallbackTitle,
                    scheduledTimeMillis = System.currentTimeMillis(),
                    status = TaskEntity.STATUS_PENDING,
                    createdAtMillis = System.currentTimeMillis()
                )

                // Only trigger if still PENDING or SNOOZED
                if (task.status == TaskEntity.STATUS_PENDING || task.status == TaskEntity.STATUS_SNOOZED) {
                    AlertQueueManager.enqueueTaskAlert(appContext, task)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
