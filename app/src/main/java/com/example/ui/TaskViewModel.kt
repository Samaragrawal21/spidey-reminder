package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alert.AlarmScheduler
import com.example.alert.AlertQueueManager
import com.example.data.SpideyDatabase
import com.example.data.TaskEntity
import com.example.data.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class HistoryFilter(val labelRes: Int, val durationMillis: Long) {
    LAST_1_DAY(com.example.R.string.filter_last_1_day, 1L * 24 * 60 * 60 * 1000L),
    LAST_7_DAYS(com.example.R.string.filter_last_7_days, 7L * 24 * 60 * 60 * 1000L),
    LAST_30_DAYS(com.example.R.string.filter_last_30_days, 30L * 24 * 60 * 60 * 1000L),
    LAST_6_MONTHS(com.example.R.string.filter_last_6_months, 180L * 24 * 60 * 60 * 1000L)
}

class TaskViewModel(
    application: Application,
    private val repository: TaskRepository
) : AndroidViewModel(application) {

    private val _selectedHistoryFilter = MutableStateFlow(HistoryFilter.LAST_7_DAYS)
    val selectedHistoryFilter: StateFlow<HistoryFilter> = _selectedHistoryFilter.asStateFlow()

    val pendingTasks: StateFlow<List<TaskEntity>> = repository.getPendingTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyTasks: StateFlow<List<TaskEntity>> = _selectedHistoryFilter
        .flatMapLatest { filter ->
            val sinceMillis = System.currentTimeMillis() - filter.durationMillis
            repository.getHistoryTasks(sinceMillis)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun setHistoryFilter(filter: HistoryFilter) {
        _selectedHistoryFilter.value = filter
    }

    fun saveOrUpdateTask(
        existingTask: TaskEntity?,
        title: String,
        scheduledTimeMillis: Long
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return

        val appContext = getApplication<Application>().applicationContext

        viewModelScope.launch {
            if (existingTask == null) {
                val now = System.currentTimeMillis()
                val newTask = TaskEntity(
                    title = cleanTitle,
                    scheduledTimeMillis = scheduledTimeMillis,
                    status = TaskEntity.STATUS_PENDING,
                    createdAtMillis = now
                )
                val generatedId = repository.insertTask(newTask)
                val persistedTask = newTask.copy(id = generatedId)
                AlarmScheduler.scheduleExactTaskAlarm(appContext, persistedTask)
            } else {
                val updatedTask = existingTask.copy(
                    title = cleanTitle,
                    scheduledTimeMillis = scheduledTimeMillis,
                    status = TaskEntity.STATUS_PENDING
                )
                repository.updateTask(updatedTask)
                AlarmScheduler.scheduleExactTaskAlarm(appContext, updatedTask)
            }
        }
    }

    fun markTaskCompletedEarly(task: TaskEntity) {
        val appContext = getApplication<Application>().applicationContext
        viewModelScope.launch {
            AlarmScheduler.cancelTaskAlarm(appContext, task.id)
            repository.updateTask(task.copy(status = TaskEntity.STATUS_COMPLETED))
        }
    }

    fun deleteTaskById(taskId: Long) {
        val appContext = getApplication<Application>().applicationContext
        viewModelScope.launch {
            AlarmScheduler.cancelTaskAlarm(appContext, taskId)
            repository.deleteTaskById(taskId)
        }
    }

    fun triggerDiagnosticAlert() {
        val appContext = getApplication<Application>().applicationContext
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val testTask = TaskEntity(
                title = "Spider-Sense Check: Symbiote Alert Engine Active",
                scheduledTimeMillis = now,
                status = TaskEntity.STATUS_PENDING,
                createdAtMillis = now
            )
            val id = repository.insertTask(testTask)
            val savedTask = testTask.copy(id = id)
            AlertQueueManager.enqueueTaskAlert(appContext, savedTask)
        }
    }

    companion object {
        /**
         * Combines the explicitly selected calendar date (year, month [0-11], dayOfMonth [1-31])
         * with the manual 12-hour keyboard time inputs (hour12 [1-12], minute [0-59], isPm)
         * to compute the exact scheduledTimeMillis without any automatic tomorrow rollover.
         */
        fun computeScheduledTimeMillis(
            year: Int,
            month: Int,
            dayOfMonth: Int,
            hour12: Int,
            minute: Int,
            isPm: Boolean
        ): Long {
            val safeHour12 = hour12.coerceIn(1, 12)
            val safeMinute = minute.coerceIn(0, 59)
            val hour24 = when {
                isPm && safeHour12 < 12 -> safeHour12 + 12
                !isPm && safeHour12 == 12 -> 0
                else -> safeHour12
            }

            return Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, hour24)
                set(Calendar.MINUTE, safeMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        /**
         * Formats the task schedule label for ActiveTaskCard:
         * - Today: "Today • 08:30 PM"
         * - Tomorrow: "Tomorrow • 08:30 PM"
         * - Further out: "MMM d • hh:mm a" (e.g., "Oct 25 • 04:30 PM")
         */
        fun formatTaskCardScheduleLabel(
            scheduledTimeMillis: Long,
            nowMillis: Long = System.currentTimeMillis()
        ): String {
            val todayCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val tomorrowCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val targetCal = Calendar.getInstance().apply { timeInMillis = scheduledTimeMillis }

            val timePart = SimpleDateFormat("hh:mm a", Locale.US).format(Date(scheduledTimeMillis))

            val isToday = todayCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                todayCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)

            val isTomorrow = tomorrowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                tomorrowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)

            return when {
                isToday -> "Today • $timePart"
                isTomorrow -> "Tomorrow • $timePart"
                else -> {
                    val datePart = SimpleDateFormat("MMM d", Locale.US).format(Date(scheduledTimeMillis))
                    "$datePart • $timePart"
                }
            }
        }

        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val dao = SpideyDatabase.getInstance(application).taskDao()
                    val repository = TaskRepository(dao)
                    return TaskViewModel(application, repository) as T
                }
            }
        }
    }
}
