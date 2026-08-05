package com.mitra.learning.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "raw_page_text",
    indices = [
        Index(value = ["sourceKey"]),
        Index(value = ["sourceKey", "pageNumber"], unique = true),
    ],
)
data class RawPageTextEntity(
    @PrimaryKey val id: String,
    val sourceKey: String,
    val pageNumber: Int,
    val text: String,
    val extractionMethod: String,
    val extractionVersion: Int,
    val confidence: Float?,
    val extractedAt: Long,
)
