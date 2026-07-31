package com.mitra.learning.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ChapterAnalysisStatus {
    NOT_PREPARED,
    PREPARING,
    READY,
    FAILED,
}

@Entity(
    tableName = "chapters",
    indices = [Index(value = ["bookId"]), Index(value = ["bookId", "startPage"])],
)
data class ChapterEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterNumber: Int?,
    val titleGujarati: String,
    val titleEnglish: String?,
    val startPage: Int,
    val endPage: Int,
    val analysisStatus: ChapterAnalysisStatus,
)
