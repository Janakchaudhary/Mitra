package com.mitra.learning.study.practice

import com.mitra.learning.ai.AiGateway
import com.mitra.learning.ai.PracticeContext
import com.mitra.learning.data.db.dao.BookDao
import com.mitra.learning.data.db.dao.ChapterDao
import com.mitra.learning.data.db.dao.ConceptDao
import com.mitra.learning.data.db.dao.PageKnowledgeDao
import com.mitra.learning.data.db.entity.ChapterAnalysisStatus
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion
import com.mitra.learning.learning.offline.OfflineQuestionBank
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Creates and evaluates short voice-friendly Standard 2 challenges.
 *
 * Prepared-book questions are loaded from the same grounded offline question bank created while
 * preparing a chapter. If the bank is empty, the configured AI provider gets one chance to build
 * questions from the saved page text; a conservative concept question is the final offline fallback.
 */
class MitraVoicePracticeService(
    private val conceptDao: ConceptDao,
    private val chapterDao: ChapterDao,
    private val bookDao: BookDao,
    private val pageKnowledgeDao: PageKnowledgeDao,
    private val questionBank: OfflineQuestionBank,
    private val aiGateway: AiGateway,
) {
    private val sequence = AtomicInteger(0)

    suspend fun nextChallenge(
        requestedTopic: MitraPracticeTopic,
        previousChallengeId: String? = null,
    ): MitraVoiceChallenge {
        val topic = if (requestedTopic == MitraPracticeTopic.MIXED) nextMixedTopic() else requestedTopic
        return when (topic) {
            MitraPracticeTopic.BOOK -> bookChallenge(previousChallengeId)
            MitraPracticeTopic.TABLES -> tableChallenge(previousChallengeId)
            MitraPracticeTopic.NUMBER_NEIGHBORS -> numberNeighborChallenge(previousChallengeId)
            MitraPracticeTopic.SPELLING -> spellingChallenge(previousChallengeId)
            MitraPracticeTopic.MIXED -> error("Mixed topic must be resolved before challenge creation")
        }
    }

    fun detectTopic(request: String): MitraPracticeTopic? {
        val value = normalize(request)
        return when {
            MIXED_REQUESTS.any(value::contains) -> MitraPracticeTopic.MIXED
            SPELLING_REQUESTS.any(value::contains) -> MitraPracticeTopic.SPELLING
            TABLE_REQUESTS.any(value::contains) && ASK_REQUESTS.any(value::contains) -> MitraPracticeTopic.TABLES
            NUMBER_REQUESTS.any(value::contains) && ASK_REQUESTS.any(value::contains) -> MitraPracticeTopic.NUMBER_NEIGHBORS
            BOOK_REQUESTS.any(value::contains) && ASK_REQUESTS.any(value::contains) -> MitraPracticeTopic.BOOK
            else -> null
        }
    }

    fun evaluate(
        challenge: MitraVoiceChallenge,
        rawAnswer: String,
        correctStreak: Int,
    ): MitraPracticeEvaluation = MitraPracticeEvaluator.evaluate(challenge, rawAnswer, correctStreak)

    fun revealedCorrection(challenge: MitraVoiceChallenge): String = challenge.correctionGujarati

    private fun nextMixedTopic(): MitraPracticeTopic {
        val topics = listOf(
            MitraPracticeTopic.TABLES,
            MitraPracticeTopic.NUMBER_NEIGHBORS,
            MitraPracticeTopic.SPELLING,
            MitraPracticeTopic.BOOK,
        )
        return topics[Math.floorMod(sequence.getAndIncrement(), topics.size)]
    }

    private fun tableChallenge(previousId: String?): MitraVoiceChallenge {
        val index = sequence.getAndIncrement()
        val table = 2 + Math.floorMod(index, 9)
        var multiplier = 1 + Math.floorMod(index * 3 + 4, 10)
        val duplicateId = "table-$table-$multiplier"
        if (duplicateId == previousId) multiplier = multiplier % 10 + 1
        val expected = table * multiplier
        return MitraVoiceChallenge(
            id = "table-$table-$multiplier",
            topic = MitraPracticeTopic.TABLES,
            kind = MitraChallengeKind.TABLE,
            promptGujarati = "કહો: $table ગુણ્યા $multiplier કેટલા?",
            evaluationMode = MitraChallengeEvaluationMode.NUMERIC,
            expectedNumber = expected,
            hintGujarati = "$table ને $multiplier વાર ઉમેરો.",
            correctionGujarati = "$table ગુણ્યા $multiplier એટલે $table ને $multiplier વાર ઉમેરવું. સાચો જવાબ $expected છે.",
        )
    }

    private fun numberNeighborChallenge(previousId: String?): MitraVoiceChallenge {
        val index = sequence.getAndIncrement()
        val number = 2 + Math.floorMod(index * 7 + 11, 97)
        val askAfter = index % 2 == 0
        val id = if (askAfter) "after-$number" else "before-$number"
        if (id == previousId) return numberNeighborChallenge(null)
        val expected = if (askAfter) number + 1 else number - 1
        return MitraVoiceChallenge(
            id = id,
            topic = MitraPracticeTopic.NUMBER_NEIGHBORS,
            kind = if (askAfter) MitraChallengeKind.AFTER_NUMBER else MitraChallengeKind.BEFORE_NUMBER,
            promptGujarati = if (askAfter) "$number પછી કઈ સંખ્યા આવે?" else "$number પહેલાં કઈ સંખ્યા આવે?",
            evaluationMode = MitraChallengeEvaluationMode.NUMERIC,
            expectedNumber = expected,
            hintGujarati = if (askAfter) "પછીની સંખ્યા માટે એક આગળ ગણો." else "પહેલાની સંખ્યા માટે એક પાછળ ગણો.",
            correctionGujarati = if (askAfter) {
                "$number પછીની સંખ્યા શોધવા એક ઉમેરો: $number + 1 = $expected."
            } else {
                "$number પહેલાંની સંખ્યા શોધવા એક ઘટાડો: $number − 1 = $expected."
            },
        )
    }

    private fun spellingChallenge(previousId: String?): MitraVoiceChallenge {
        var index = Math.floorMod(sequence.getAndIncrement(), EnglishSpellingLexicon.standard2Words.size)
        var word = EnglishSpellingLexicon.standard2Words[index]
        if ("spelling-${word.word}" == previousId) {
            index = (index + 1) % EnglishSpellingLexicon.standard2Words.size
            word = EnglishSpellingLexicon.standard2Words[index]
        }
        return MitraVoiceChallenge(
            id = "spelling-${word.word}",
            topic = MitraPracticeTopic.SPELLING,
            kind = MitraChallengeKind.SPELLING,
            promptGujarati = "${word.word.uppercase()} (${word.meaningGujarati}) નો spelling બોલો.",
            spokenPrompt = "Spell the word ${word.word}",
            speechLanguageTag = "en-IN",
            recognitionLanguageTag = "en-IN",
            evaluationMode = MitraChallengeEvaluationMode.EXACT_TEXT,
            expectedText = word.word,
            acceptedAnswers = listOf(word.word, word.word.toCharArray().joinToString(" ")),
            hintGujarati = "શબ્દને ધીમે અક્ષર-અક્ષર બોલો.",
            correctionGujarati = "${word.word.uppercase()} નો સાચો spelling ${EnglishSpellingLexicon.letters(word.word)} છે.",
        )
    }

    private suspend fun bookChallenge(previousId: String?): MitraVoiceChallenge {
        val chapters = chapterDao.getAll()
            .filter { it.analysisStatus == ChapterAnalysisStatus.READY }
            .associateBy { it.id }
        val concepts = conceptDao.getPracticeReady()
            .filter { !it.builtIn && it.chapterId in chapters.keys }
        if (concepts.isEmpty()) return noPreparedBookChallenge()

        val start = Math.floorMod(sequence.getAndIncrement(), concepts.size)
        val ordered = concepts.drop(start) + concepts.take(start)
        for (concept in ordered) {
            val chapter = concept.chapterId?.let(chapters::get) ?: continue
            val book = concept.bookId?.let { bookDao.findById(it) }
            val sourceLabel = listOfNotNull(
                book?.title,
                chapter.titleGujarati,
                concept.sourcePageStart?.let { "p.$it" },
            ).joinToString(" • ")

            var candidates = questionBank.load(concept.id, 20)
                .filter(::isVoiceEvaluatable)
                .filter { "book-${it.id}" != previousId }

            if (candidates.isEmpty()) {
                val pages = pageKnowledgeDao.forChapter(chapter.id)
                    .filter { page ->
                        val startPage = concept.sourcePageStart ?: chapter.startPage
                        val endPage = concept.sourcePageEnd ?: chapter.endPage
                        page.pageNumber in startPage..endPage
                    }
                val groundedText = pages.joinToString("\n\n") { page ->
                    buildString {
                        append("Page ${page.pageNumber}: ${page.summaryGujarati}")
                        page.visibleTextGujarati?.takeIf(String::isNotBlank)?.let { append("\nText: $it") }
                        page.exercisesJson?.takeIf(String::isNotBlank)?.let { append("\nExercises: $it") }
                    }
                }.take(12_000)
                val generated = runCatching {
                    aiGateway.createPracticeQuestions(
                        concept = concept,
                        count = 6,
                        context = PracticeContext(
                            bookTitle = book?.title,
                            chapterTitleGujarati = chapter.titleGujarati,
                            groundedBookText = groundedText,
                        ),
                    )
                }.getOrDefault(emptyList())
                    .map { it.copy(conceptId = it.conceptId ?: concept.id) }
                    .filter(::isVoiceEvaluatable)
                if (generated.isNotEmpty()) {
                    questionBank.save(concept.id, generated)
                    candidates = generated
                }
            }

            candidates.firstOrNull()?.let { question ->
                return question.toVoiceChallenge(sourceLabel)
            }

            return conceptFallbackChallenge(concept, sourceLabel, previousId)
        }
        return noPreparedBookChallenge()
    }

    private fun LearningQuestion.toVoiceChallenge(sourceLabel: String): MitraVoiceChallenge {
        val mode = when (evaluationMode) {
            EvaluationMode.NUMERIC -> MitraChallengeEvaluationMode.NUMERIC
            EvaluationMode.KEYWORD -> MitraChallengeEvaluationMode.KEYWORD
            EvaluationMode.MULTIPLE_CHOICE,
            EvaluationMode.SHORT_TEXT,
            -> MitraChallengeEvaluationMode.EXACT_TEXT
            EvaluationMode.PARTICIPATION -> error("Participation question cannot become a voice challenge")
        }
        val expectedDisplay = expectedAnswer?.toString()
            ?: expectedText
            ?: acceptedAnswers.firstOrNull()
            ?: "પુસ્તકમાં આપેલો જવાબ"
        return MitraVoiceChallenge(
            id = "book-$id",
            topic = MitraPracticeTopic.BOOK,
            kind = MitraChallengeKind.BOOK,
            promptGujarati = promptGujarati,
            spokenPrompt = spokenPromptGujarati ?: promptGujarati,
            speechLanguageTag = speechLanguageTag ?: "gu-IN",
            recognitionLanguageTag = recognitionLanguageTag ?: "gu-IN",
            evaluationMode = mode,
            expectedNumber = expectedAnswer,
            expectedText = expectedText,
            acceptedAnswers = acceptedAnswers,
            hintGujarati = hintGujarati ?: "તૈયાર પાઠનો સંબંધિત ભાગ યાદ કરો.",
            correctionGujarati = "પુસ્તક મુજબ યોગ્ય જવાબ: $expectedDisplay. ${hintGujarati.orEmpty()}".trim(),
            sourceLabels = listOf(sourceLabel).filter(String::isNotBlank),
        )
    }

    private fun conceptFallbackChallenge(
        concept: ConceptEntity,
        sourceLabel: String,
        previousId: String?,
    ): MitraVoiceChallenge {
        val id = "book-concept-${concept.id}"
        val title = concept.titleGujarati.trim()
        val accepted = titleKeywords(title)
        val alternatePrompt = "આ પાઠનો મુખ્ય વિષય પોતાના શબ્દોમાં કહો: ${concept.descriptionGujarati.take(150)}"
        return MitraVoiceChallenge(
            id = if (id == previousId) "$id-${sequence.getAndIncrement()}" else id,
            topic = MitraPracticeTopic.BOOK,
            kind = MitraChallengeKind.BOOK,
            promptGujarati = alternatePrompt,
            evaluationMode = MitraChallengeEvaluationMode.KEYWORD,
            expectedText = title,
            acceptedAnswers = accepted,
            hintGujarati = "પાઠનું નામ '$title' યાદ કરો.",
            correctionGujarati = "આ પાઠનો મુખ્ય વિષય '$title' છે. ${concept.expectedLearningOutcome}",
            sourceLabels = listOf(sourceLabel).filter(String::isNotBlank),
        )
    }

    private fun noPreparedBookChallenge(): MitraVoiceChallenge = MitraVoiceChallenge(
        id = "book-unavailable",
        topic = MitraPracticeTopic.BOOK,
        kind = MitraChallengeKind.BOOK,
        promptGujarati = "પુસ્તક પ્રશ્ન માટે Parent mode માં પહેલાં ઓછામાં ઓછો એક પાઠ Prepare કરો. હમણાં ઘડિયા, પહેલાં-પછી અથવા spelling પસંદ કરો.",
        evaluationMode = MitraChallengeEvaluationMode.KEYWORD,
        expectedText = "",
        correctionGujarati = "Parent mode માં પુસ્તકનો પાઠ Prepare કર્યા પછી ફરી પુસ્તક પ્રશ્ન પસંદ કરો.",
    )

    private fun isVoiceEvaluatable(question: LearningQuestion): Boolean = when (question.evaluationMode) {
        EvaluationMode.NUMERIC -> question.expectedAnswer != null
        EvaluationMode.MULTIPLE_CHOICE,
        EvaluationMode.SHORT_TEXT,
        EvaluationMode.KEYWORD,
        -> !question.expectedText.isNullOrBlank() || question.acceptedAnswers.isNotEmpty()
        EvaluationMode.PARTICIPATION -> false
    }

    private fun titleKeywords(title: String): List<String> = SpokenAnswerNormalizer.text(title)
        .split(' ')
        .filter { it.length >= 2 && it !in TITLE_STOP_WORDS }
        .take(5)
        .ifEmpty { listOf(title) }

    private fun normalize(value: String): String = SpokenAnswerNormalizer.text(value)

    private companion object {
        val ASK_REQUESTS = listOf("પૂછ", "ક્વિઝ", "રમત", "પ્રશ્ન", "ask", "quiz", "practice")
        val MIXED_REQUESTS = listOf("મિશ્ર", "બધું પૂછ", "મિક્સ", "mixed quiz")
        val SPELLING_REQUESTS = listOf("spelling", "સ્પેલિંગ", "જોડણી", "અક્ષર પૂછ")
        val TABLE_REQUESTS = listOf("ઘડિયા", "ઘડિયો", "ગડિયા", "પહાડો", "ટેબલ", "table")
        val NUMBER_REQUESTS = listOf("પછીની સંખ્યા", "પહેલાની સંખ્યા", "પહેલાં પછી", "આગળ પાછળ", "before after")
        val BOOK_REQUESTS = listOf("પુસ્તકમાંથી", "પાઠમાંથી", "બુકમાંથી", "book question", "પુસ્તક પ્રશ્ન")
        val TITLE_STOP_WORDS = setOf("અને", "ના", "ની", "નું", "એક", "the", "and")
    }
}
