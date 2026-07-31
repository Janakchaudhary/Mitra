package com.mitra.learning.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "page_knowledge",
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["chapterId"]),
        Index(value = ["bookId", "pageNumber"], unique = true),
    ],
)
data class PageKnowledgeEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterId: String,
    val pageNumber: Int,
    val summaryGujarati: String,
    val visibleTextGujarati: String?,
    val importantObjectsJson: String?,
    val exercisesJson: String?,
    val conceptsJson: String?,
    val analyzedAt: Long,
)
