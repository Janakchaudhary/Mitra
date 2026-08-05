package com.mitra.learning.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parent_quiz_plans")
data class ParentQuizPlanEntity(
    @PrimaryKey val id: String,
    val title: String,
    val topic: String,
    val createdAt: Long,
    val skillConceptId: String?,
    val skillTitleGujarati: String?,
    val bookId: String?,
    val bookTitle: String?,
    val chapterId: String?,
    val chapterTitleGujarati: String?,
)
