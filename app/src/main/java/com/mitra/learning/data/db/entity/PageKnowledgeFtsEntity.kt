package com.mitra.learning.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4
@Entity(tableName = "page_knowledge_fts")
data class PageKnowledgeFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long,
    val pageKnowledgeId: String,
    val bookId: String,
    val chapterId: String,
    val pageNumberText: String,
    val content: String,
)
