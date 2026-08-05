package com.mitra.learning.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "preparation_jobs",
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["chapterId"]),
        Index(value = ["status"]),
    ],
)
data class PreparationJobEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterId: String,
    val status: String,
    val progressPercent: Int,
    val currentStageGujarati: String,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
