package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.SpideyDatabase
import com.example.data.TaskDao
import com.example.data.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: SpideyDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SpideyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun pendingAndSnoozedAppearOnlyInActive_completedAndMissedAppearOnlyInHistory() = runBlocking {
        val now = System.currentTimeMillis()

        val pendingId = taskDao.insertTask(
            TaskEntity(
                title = "Patrol Queens",
                scheduledTimeMillis = now + 600_000L,
                status = TaskEntity.STATUS_PENDING,
                createdAtMillis = now
            )
        )
        val snoozedId = taskDao.insertTask(
            TaskEntity(
                title = "Refill Web Fluid",
                scheduledTimeMillis = now + 900_000L,
                status = TaskEntity.STATUS_SNOOZED,
                createdAtMillis = now
            )
        )
        val completedId = taskDao.insertTask(
            TaskEntity(
                title = "Submit Daily Bugle Photos",
                scheduledTimeMillis = now - 300_000L,
                status = TaskEntity.STATUS_COMPLETED,
                createdAtMillis = now
            )
        )
        val missedId = taskDao.insertTask(
            TaskEntity(
                title = "Dr. Connors Lab Session",
                scheduledTimeMillis = now - 600_000L,
                status = TaskEntity.STATUS_MISSED,
                createdAtMillis = now
            )
        )

        val activeList = taskDao.getPendingTasks().first()
        assertEquals(2, activeList.size)
        assertEquals(listOf(pendingId, snoozedId), activeList.map { it.id })

        val historyList = taskDao.getHistoryTasks(now - 86_400_000L).first()
        assertEquals(2, historyList.size)
        assertTrue(historyList.none { it.status == TaskEntity.STATUS_SNOOZED })
        assertEquals(setOf(completedId, missedId), historyList.map { it.id }.toSet())

        // Verify single-item delete
        taskDao.deleteTaskById(completedId)
        val updatedHistory = taskDao.getHistoryTasks(now - 86_400_000L).first()
        assertEquals(1, updatedHistory.size)
        assertEquals(missedId, updatedHistory.first().id)
    }

    @Test
    fun audioAssetManager_resolvesCorrectInternalStorageFileAndUrl() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundFile = com.example.alert.AudioAssetManager.getAlertSoundFile(context)

        assertEquals(context.filesDir.resolve("black_suit_alert.mp3").absolutePath, soundFile.absolutePath)
        assertEquals("https://files.catbox.moe/zhujv9.mp3", com.example.alert.AudioAssetManager.ALERT_AUDIO_URL)

        // When the file already exists in internal storage, downloadAlertSoundIfNeeded returns it without overwriting
        soundFile.writeBytes("ID3-cached".toByteArray())
        val cachedResult = com.example.alert.AudioAssetManager.downloadAlertSoundIfNeeded(context)
        assertEquals(soundFile.absolutePath, cachedResult?.absolutePath)
        assertEquals("ID3-cached", soundFile.readText())
        soundFile.delete()
    }

    @Test
    fun computeScheduledTimeMillisAndFormatLabels_handlesTodayTomorrowAndFutureDatesWithoutRollover() {
        val baseCal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.OCTOBER, 18, 12, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val baseNowMillis = baseCal.timeInMillis

        // 1. Explicit Today in the past (08:30 AM on Oct 18) does NOT roll over to Oct 19
        val todayPastMillis = com.example.ui.TaskViewModel.computeScheduledTimeMillis(
            year = 2026,
            month = java.util.Calendar.OCTOBER,
            dayOfMonth = 18,
            hour12 = 8,
            minute = 30,
            isPm = false
        )
        assertTrue(todayPastMillis < baseNowMillis)
        assertEquals(
            "Today • 08:30 AM",
            com.example.ui.TaskViewModel.formatTaskCardScheduleLabel(todayPastMillis, baseNowMillis)
        )

        // 2. Tomorrow (08:30 PM on Oct 19)
        val tomorrowMillis = com.example.ui.TaskViewModel.computeScheduledTimeMillis(
            year = 2026,
            month = java.util.Calendar.OCTOBER,
            dayOfMonth = 19,
            hour12 = 8,
            minute = 30,
            isPm = true
        )
        assertEquals(
            "Tomorrow • 08:30 PM",
            com.example.ui.TaskViewModel.formatTaskCardScheduleLabel(tomorrowMillis, baseNowMillis)
        )

        // 3. Future Custom Date (04:30 PM on Oct 25)
        val futureCustomMillis = com.example.ui.TaskViewModel.computeScheduledTimeMillis(
            year = 2026,
            month = java.util.Calendar.OCTOBER,
            dayOfMonth = 25,
            hour12 = 4,
            minute = 30,
            isPm = true
        )
        assertEquals(
            "Oct 25 • 04:30 PM",
            com.example.ui.TaskViewModel.formatTaskCardScheduleLabel(futureCustomMillis, baseNowMillis)
        )
    }
}
