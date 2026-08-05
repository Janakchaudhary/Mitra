package com.mitra.learning.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prepared_questions",
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["chapterId"]),
        Index(value = ["conceptId"]),
        Index(value = ["chapterId", "qualityStatus"]),
        Index(value = ["fingerprint"]),
    ],
)
data class PreparedQuestionEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterId: String,
    val conceptId: String?,
    val promptGujarati: String,
    val spokenPromptGujarati: String?,
    val speechLanguageTag: String?,
    val recognitionLanguageTag: String?,
    val expectedAnswer: Int?,
    val activityType: String,
    val evaluationMode: String,
    val expectedText: String?,
    val acceptedAnswersJson: String,
    val optionsGujaratiJson: String,
    val hintGujarati: String?,
    val completionButtonGujarati: String,
    val sourcePage: Int?,
    val fingerprint: String,
    val difficulty: Int = 1,
    val qualityStatus: String = "APPROVED",
    val usedCount: Int = 0,
    val lastUsedAt: Long? = null,
)
