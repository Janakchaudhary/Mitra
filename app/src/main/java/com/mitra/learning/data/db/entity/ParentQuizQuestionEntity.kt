package com.mitra.learning.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parent_quiz_questions",
    indices = [
        Index(value = ["planId"]),
        Index(value = ["planId", "position"], unique = true),
    ],
)
data class ParentQuizQuestionEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val position: Int,
    val promptGujarati: String,
    val spokenPrompt: String,
    val recognitionLanguageTag: String,
    val kind: String,
    val evaluationMode: String,
    val expectedNumber: Int?,
    val expectedText: String?,
    val acceptedAnswersJson: String,
    val hintGujarati: String?,
    val correctionGujarati: String,
    val sourceLabelsJson: String,
)
