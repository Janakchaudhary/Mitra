package com.mitra.learning.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mastery")
data class MasteryEntity(
    @PrimaryKey val conceptId: String,
    val mastery: Float,
    val totalAttempts: Int,
    val correctAttempts: Int,
    val hintCount: Int,
    val lastPracticedAt: Long?,
    val lastSuccessAt: Long?,
)
