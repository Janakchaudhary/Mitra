package com.mitra.learning.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BookAnalysisStatus {
    NOT_ANALYZED,
    PARTIAL,
    READY,
    FAILED,
}

@Entity(
    tableName = "books",
    indices = [Index(value = ["sha256"], unique = true)]
)
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val standard: Int,
    val language: String,
    val localPdfPath: String,
    val sha256: String,
    val pageCount: Int,
    val coverPath: String?,
    val createdAt: Long,
    val analysisStatus: BookAnalysisStatus,
)
