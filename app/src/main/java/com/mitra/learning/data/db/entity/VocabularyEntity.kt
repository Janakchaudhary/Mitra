package com.mitra.learning.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vocabulary",
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["chapterId"]),
        Index(value = ["normalizedWord"]),
        Index(value = ["chapterId", "normalizedWord"], unique = true),
    ],
)
data class VocabularyEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterId: String,
    val word: String,
    val normalizedWord: String,
    val meaningGujarati: String,
    val simpleExplanationGujarati: String?,
    val exampleSentenceGujarati: String?,
    val sourcePage: Int,
    val acceptedVoiceFormsJson: String,
)
