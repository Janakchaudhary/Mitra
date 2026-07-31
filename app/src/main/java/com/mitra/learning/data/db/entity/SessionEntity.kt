package com.mitra.learning.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SessionStatus {
    ACTIVE,
    COMPLETED,
    STOPPED,
}

@Entity(tableName = "learning_sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val primaryConceptId: String?,
    val activityCount: Int,
    val durationSeconds: Int,
    val status: SessionStatus,
)
