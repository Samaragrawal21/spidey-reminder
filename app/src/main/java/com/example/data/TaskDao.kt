package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE status IN ('PENDING', 'SNOOZED') ORDER BY scheduledTimeMillis ASC")
    fun getPendingTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status IN ('COMPLETED', 'MISSED') AND createdAtMillis >= :sinceMillis ORDER BY scheduledTimeMillis DESC")
    fun getHistoryTasks(sinceMillis: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status IN ('PENDING', 'SNOOZED') ORDER BY scheduledTimeMillis ASC")
    suspend fun getActiveTasksSnapshot(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
}
