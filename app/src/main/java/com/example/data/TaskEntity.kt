package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val scheduledTimeMillis: Long,
    val status: String, // "PENDING", "COMPLETED", "MISSED", "SNOOZED"
    val createdAtMillis: Long
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_MISSED = "MISSED"
        const val STATUS_SNOOZED = "SNOOZED"
    }
}
