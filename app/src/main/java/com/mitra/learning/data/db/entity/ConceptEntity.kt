package com.mitra.learning.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "concepts",
    indices = [Index(value = ["bookId"]), Index(value = ["subject"])],
)
data class ConceptEntity(
    @PrimaryKey val id: String,
    val subject: String,
    val standard: Int,
    val language: String,
    val titleGujarati: String,
    val titleEnglish: String?,
    val descriptionGujarati: String,
    val difficulty: Int,
    val expectedLearningOutcome: String,
    val sortOrder: Int,
    val builtIn: Boolean,
    val bookId: String?,
    val chapterId: String?,
    val sourcePageStart: Int?,
    val sourcePageEnd: Int?,
)
