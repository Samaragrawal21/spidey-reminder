package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    fun getPendingTasks(): Flow<List<TaskEntity>> = taskDao.getPendingTasks()

    fun getHistoryTasks(sinceMillis: Long): Flow<List<TaskEntity>> =
        taskDao.getHistoryTasks(sinceMillis)

    suspend fun getActiveTasksSnapshot(): List<TaskEntity> =
        taskDao.getActiveTasksSnapshot()

    suspend fun getTaskById(id: Long): TaskEntity? =
        taskDao.getTaskById(id)

    suspend fun insertTask(task: TaskEntity): Long =
        taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) =
        taskDao.updateTask(task)

    suspend fun deleteTaskById(id: Long) =
        taskDao.deleteTaskById(id)
}
