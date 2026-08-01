package com.mitra.learning.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttemptResult {
    CORRECT,
    PARTIAL,
    INCORRECT,
    SKIPPED,
    UNKNOWN,
}

@Entity(
    tableName = "attempts",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["conceptId"]),
        Index(value = ["questionFingerprint"]),
    ],
)
data class AttemptEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val conceptId: String,
    val activityType: String,
    val result: AttemptResult,
    val hintCount: Int,
    val createdAt: Long,
    val questionFingerprint: String = "",
)
