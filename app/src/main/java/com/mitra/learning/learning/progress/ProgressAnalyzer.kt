package com.mitra.learning.learning.progress

import com.mitra.learning.data.db.entity.AttemptEntity
import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.ConceptPrerequisiteEntity
import com.mitra.learning.data.db.entity.MasteryEntity
import com.mitra.learning.data.db.entity.SessionEntity
import com.mitra.learning.data.db.entity.SessionStatus
import java.time.Instant
import java.time.ZoneId

object ProgressAnalyzer {
    private const val NEEDS_PRACTICE_THRESHOLD = 0.60f
    private const val STRONG_THRESHOLD = 0.85f
    private const val RECENT_SESSION_LIMIT = 8
    private const val PREREQUISITE_THRESHOLD = 0.70f

    fun analyze(
        concepts: List<ConceptEntity>,
        mastery: List<MasteryEntity>,
        prerequisites: List<ConceptPrerequisiteEntity> = emptyList(),
        sessions: List<SessionEntity>,
        attempts: List<AttemptEntity>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): ProgressDashboard {
        val conceptById = concepts.associateBy { it.id }
        val masteryById = mastery.associateBy { it.conceptId }
        val attemptsBySession = attempts.groupBy { it.sessionId }
        val prerequisitesByConcept = prerequisites.groupBy { it.conceptId }
        val sevenDaysAgo = nowMillis - 7L * 24L * 60L * 60L * 1000L
        val todayStart = Instant.ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val completed = sessions.filter { it.status == SessionStatus.COMPLETED }
        val finalized = sessions.filter { it.status != SessionStatus.ACTIVE }
        val assessedAttempts = attempts.filter { it.result != AttemptResult.UNKNOWN }
        val correctAttempts = assessedAttempts.count { it.result == AttemptResult.CORRECT }

        val conceptProgress = concepts
            .filter { it.practiceReady }
            .map { concept ->
                val item = masteryById[concept.id]
                ConceptProgress(
                    conceptId = concept.id,
                    titleGujarati = concept.titleGujarati,
                    subject = concept.subject,
                    mastery = item?.mastery ?: 0f,
                    attempts = item?.totalAttempts ?: 0,
                    correctAttempts = item?.correctAttempts ?: 0,
                    hints = item?.hintCount ?: 0,
                    lastPracticedAt = item?.lastPracticedAt,
                    fromBook = !concept.builtIn,
                )
            }

        val practiced = conceptProgress.filter { it.attempts > 0 || it.lastPracticedAt != null }
        val overall = practiced.takeIf { it.isNotEmpty() }?.map { it.mastery }?.average()?.toFloat() ?: 0f

        val subjects = conceptProgress
            .groupBy { it.subject }
            .map { (subject, items) ->
                val subjectPracticed = items.filter { it.attempts > 0 || it.lastPracticedAt != null }
                SubjectProgress(
                    subject = subject,
                    mastery = subjectPracticed.takeIf { it.isNotEmpty() }
                        ?.map { it.mastery }
                        ?.average()
                        ?.toFloat()
                        ?: 0f,
                    practicedConcepts = subjectPracticed.size,
                    totalAttempts = subjectPracticed.sumOf { it.attempts },
                )
            }
            .sortedWith(compareByDescending<SubjectProgress> { it.practicedConcepts }.thenBy { it.subject })

        val needsPractice = practiced
            .filter { it.mastery < NEEDS_PRACTICE_THRESHOLD }
            .sortedWith(compareBy<ConceptProgress> { it.mastery }.thenByDescending { it.attempts })
            .take(5)

        val strong = practiced
            .filter { it.mastery >= STRONG_THRESHOLD }
            .sortedWith(compareByDescending<ConceptProgress> { it.mastery }.thenByDescending { it.attempts })
            .take(5)

        val recent = sessions
            .sortedByDescending { it.startedAt }
            .take(RECENT_SESSION_LIMIT)
            .map { session ->
                val sessionAttempts = attemptsBySession[session.id].orEmpty()
                val assessed = sessionAttempts.filter { it.result != AttemptResult.UNKNOWN }
                RecentSessionProgress(
                    id = session.id,
                    startedAt = session.startedAt,
                    durationSeconds = session.durationSeconds,
                    status = session.status,
                    conceptTitleGujarati = session.primaryConceptId
                        ?.let { conceptById[it]?.titleGujarati }
                        ?: "અભ્યાસ",
                    assessed = assessed.size,
                    correct = assessed.count { it.result == AttemptResult.CORRECT },
                    participation = sessionAttempts.count { it.result == AttemptResult.UNKNOWN },
                )
            }

        // Recommendation follows the same prerequisite boundary as the learning engine.
        // Prefer prepared textbook concepts when available, weak practiced work first, then new
        // eligible concepts, and only then mastered review.
        val eligibleRecommendationIds = concepts
            .filter { it.practiceReady }
            .filter { concept ->
                prerequisitesByConcept[concept.id].orEmpty().all { link ->
                    (masteryById[link.prerequisiteConceptId]?.mastery ?: 0f) >= PREREQUISITE_THRESHOLD
                }
            }
            .mapTo(mutableSetOf()) { it.id }
        val eligibleProgress = conceptProgress.filter { it.conceptId in eligibleRecommendationIds }
        val recommendationPool = eligibleProgress.filter { it.fromBook }.ifEmpty { eligibleProgress }
        val recommendation = recommendationPool
            .filter { it.attempts > 0 && it.mastery < 0.85f }
            .minWithOrNull(compareBy<ConceptProgress> { it.mastery }.thenBy { it.lastPracticedAt ?: Long.MIN_VALUE })
            ?: recommendationPool
                .filter { it.attempts == 0 }
                .minByOrNull { it.conceptId }
            ?: recommendationPool
                .minWithOrNull(compareBy<ConceptProgress> { it.lastPracticedAt ?: Long.MIN_VALUE }.thenBy { it.mastery })

        return ProgressDashboard(
            todayMinutes = finalized.filter { it.startedAt >= todayStart }
                .sumOf { it.durationSeconds }
                .secondsToRoundedMinutes(),
            last7DaysMinutes = finalized.filter { it.startedAt >= sevenDaysAgo }
                .sumOf { it.durationSeconds }
                .secondsToRoundedMinutes(),
            completedSessions = completed.size,
            assessedAttempts = assessedAttempts.size,
            correctAttempts = correctAttempts,
            overallMastery = overall,
            subjects = subjects,
            needsPractice = needsPractice,
            strongConcepts = strong,
            recentSessions = recent,
            recommendation = recommendation,
        )
    }

    private fun Int.secondsToRoundedMinutes(): Int = if (this <= 0) 0 else (this + 59) / 60
}
