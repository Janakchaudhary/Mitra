package com.mitra.learning.learning.progress

import com.mitra.learning.data.db.entity.SessionStatus

data class ProgressDashboard(
    val todayMinutes: Int,
    val last7DaysMinutes: Int,
    val completedSessions: Int,
    val assessedAttempts: Int,
    val correctAttempts: Int,
    val overallMastery: Float,
    val subjects: List<SubjectProgress>,
    val needsPractice: List<ConceptProgress>,
    val strongConcepts: List<ConceptProgress>,
    val standard2Skills: List<ConceptProgress>,
    val recentSessions: List<RecentSessionProgress>,
    val recommendation: ConceptProgress?,
    val weeklyReport: WeeklyReport,
)

data class SubjectProgress(
    val subject: String,
    val mastery: Float,
    val practicedConcepts: Int,
    val totalAttempts: Int,
)

data class ConceptProgress(
    val conceptId: String,
    val titleGujarati: String,
    val subject: String,
    val mastery: Float,
    val attempts: Int,
    val correctAttempts: Int,
    val hints: Int,
    val lastPracticedAt: Long?,
    val fromBook: Boolean,
    val nextReviewAt: Long? = null,
)

data class RecentSessionProgress(
    val id: String,
    val startedAt: Long,
    val durationSeconds: Int,
    val status: SessionStatus,
    val conceptTitleGujarati: String,
    val assessed: Int,
    val correct: Int,
    val participation: Int,
)


data class WeeklyReport(
    val minutes: Int,
    val assessed: Int,
    val correct: Int,
    val mostPracticedTitleGujarati: String?,
    val needsPracticeTitleGujarati: String?,
    val dueReviewCount: Int,
)
