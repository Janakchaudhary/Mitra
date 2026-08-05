package com.mitra.learning.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mitra.learning.data.db.dao.AttemptDao
import com.mitra.learning.data.db.dao.BookDao
import com.mitra.learning.data.db.dao.ChapterDao
import com.mitra.learning.data.db.dao.ConceptDao
import com.mitra.learning.data.db.dao.MasteryDao
import com.mitra.learning.data.db.dao.PageKnowledgeDao
import com.mitra.learning.data.db.dao.PageKnowledgeFtsDao
import com.mitra.learning.data.db.dao.ParentQuizDao
import com.mitra.learning.data.db.dao.PreparedQuestionDao
import com.mitra.learning.data.db.dao.PreparationJobDao
import com.mitra.learning.data.db.dao.RawPageTextDao
import com.mitra.learning.data.db.dao.VocabularyDao
import com.mitra.learning.data.db.dao.SessionDao
import com.mitra.learning.data.db.entity.AttemptEntity
import com.mitra.learning.data.db.entity.BookEntity
import com.mitra.learning.data.db.entity.ChapterEntity
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.ConceptPrerequisiteEntity
import com.mitra.learning.data.db.entity.MasteryEntity
import com.mitra.learning.data.db.entity.PageKnowledgeEntity
import com.mitra.learning.data.db.entity.PageKnowledgeFtsEntity
import com.mitra.learning.data.db.entity.ParentQuizPlanEntity
import com.mitra.learning.data.db.entity.ParentQuizQuestionEntity
import com.mitra.learning.data.db.entity.PreparedQuestionEntity
import com.mitra.learning.data.db.entity.PreparationJobEntity
import com.mitra.learning.data.db.entity.RawPageTextEntity
import com.mitra.learning.data.db.entity.VocabularyEntity
import com.mitra.learning.data.db.entity.SessionEntity

@Database(
    entities = [
        BookEntity::class,
        ConceptEntity::class,
        ConceptPrerequisiteEntity::class,
        MasteryEntity::class,
        SessionEntity::class,
        AttemptEntity::class,
        ChapterEntity::class,
        PageKnowledgeEntity::class,
        PageKnowledgeFtsEntity::class,
        VocabularyEntity::class,
        PreparedQuestionEntity::class,
        RawPageTextEntity::class,
        PreparationJobEntity::class,
        ParentQuizPlanEntity::class,
        ParentQuizQuestionEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class MitraDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun conceptDao(): ConceptDao
    abstract fun masteryDao(): MasteryDao
    abstract fun sessionDao(): SessionDao
    abstract fun attemptDao(): AttemptDao
    abstract fun chapterDao(): ChapterDao
    abstract fun pageKnowledgeDao(): PageKnowledgeDao
    abstract fun pageKnowledgeFtsDao(): PageKnowledgeFtsDao
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun preparedQuestionDao(): PreparedQuestionDao
    abstract fun rawPageTextDao(): RawPageTextDao
    abstract fun preparationJobDao(): PreparationJobDao
    abstract fun parentQuizDao(): ParentQuizDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS concepts (
                        id TEXT NOT NULL PRIMARY KEY,
                        subject TEXT NOT NULL,
                        standard INTEGER NOT NULL,
                        language TEXT NOT NULL,
                        titleGujarati TEXT NOT NULL,
                        titleEnglish TEXT,
                        descriptionGujarati TEXT NOT NULL,
                        difficulty INTEGER NOT NULL,
                        expectedLearningOutcome TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        builtIn INTEGER NOT NULL,
                        bookId TEXT,
                        chapterId TEXT,
                        sourcePageStart INTEGER,
                        sourcePageEnd INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_concepts_bookId ON concepts(bookId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_concepts_subject ON concepts(subject)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS concept_prerequisites (
                        conceptId TEXT NOT NULL,
                        prerequisiteConceptId TEXT NOT NULL,
                        PRIMARY KEY(conceptId, prerequisiteConceptId)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mastery (
                        conceptId TEXT NOT NULL PRIMARY KEY,
                        mastery REAL NOT NULL,
                        totalAttempts INTEGER NOT NULL,
                        correctAttempts INTEGER NOT NULL,
                        hintCount INTEGER NOT NULL,
                        lastPracticedAt INTEGER,
                        lastSuccessAt INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS learning_sessions (
                        id TEXT NOT NULL PRIMARY KEY,
                        startedAt INTEGER NOT NULL,
                        endedAt INTEGER,
                        primaryConceptId TEXT,
                        activityCount INTEGER NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        status TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS attempts (
                        id TEXT NOT NULL PRIMARY KEY,
                        sessionId TEXT NOT NULL,
                        conceptId TEXT NOT NULL,
                        activityType TEXT NOT NULL,
                        result TEXT NOT NULL,
                        hintCount INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attempts_sessionId ON attempts(sessionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attempts_conceptId ON attempts(conceptId)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE concepts ADD COLUMN practiceReady INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chapters (
                        id TEXT NOT NULL PRIMARY KEY,
                        bookId TEXT NOT NULL,
                        chapterNumber INTEGER,
                        titleGujarati TEXT NOT NULL,
                        titleEnglish TEXT,
                        startPage INTEGER NOT NULL,
                        endPage INTEGER NOT NULL,
                        analysisStatus TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapters_bookId ON chapters(bookId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapters_bookId_startPage ON chapters(bookId, startPage)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS page_knowledge (
                        id TEXT NOT NULL PRIMARY KEY,
                        bookId TEXT NOT NULL,
                        chapterId TEXT NOT NULL,
                        pageNumber INTEGER NOT NULL,
                        summaryGujarati TEXT NOT NULL,
                        visibleTextGujarati TEXT,
                        importantObjectsJson TEXT,
                        exercisesJson TEXT,
                        conceptsJson TEXT,
                        analyzedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_page_knowledge_bookId ON page_knowledge(bookId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_page_knowledge_chapterId ON page_knowledge(chapterId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_page_knowledge_bookId_pageNumber ON page_knowledge(bookId, pageNumber)")
            }
        }



        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attempts ADD COLUMN questionFingerprint TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attempts_questionFingerprint ON attempts(questionFingerprint)")
            }
        }


        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mastery ADD COLUMN nextReviewAt INTEGER")
                db.execSQL("ALTER TABLE mastery ADD COLUMN reviewIntervalDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE mastery ADD COLUMN consecutiveSuccesses INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS vocabulary (
                        id TEXT NOT NULL PRIMARY KEY,
                        bookId TEXT NOT NULL,
                        chapterId TEXT NOT NULL,
                        word TEXT NOT NULL,
                        normalizedWord TEXT NOT NULL,
                        meaningGujarati TEXT NOT NULL,
                        simpleExplanationGujarati TEXT,
                        exampleSentenceGujarati TEXT,
                        sourcePage INTEGER NOT NULL,
                        acceptedVoiceFormsJson TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_bookId ON vocabulary(bookId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_chapterId ON vocabulary(chapterId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_normalizedWord ON vocabulary(normalizedWord)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_vocabulary_chapterId_normalizedWord ON vocabulary(chapterId, normalizedWord)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS prepared_questions (
                        id TEXT NOT NULL PRIMARY KEY,
                        bookId TEXT NOT NULL,
                        chapterId TEXT NOT NULL,
                        conceptId TEXT,
                        promptGujarati TEXT NOT NULL,
                        spokenPromptGujarati TEXT,
                        speechLanguageTag TEXT,
                        recognitionLanguageTag TEXT,
                        expectedAnswer INTEGER,
                        activityType TEXT NOT NULL,
                        evaluationMode TEXT NOT NULL,
                        expectedText TEXT,
                        acceptedAnswersJson TEXT NOT NULL,
                        optionsGujaratiJson TEXT NOT NULL,
                        hintGujarati TEXT,
                        completionButtonGujarati TEXT NOT NULL,
                        sourcePage INTEGER,
                        fingerprint TEXT NOT NULL,
                        difficulty INTEGER NOT NULL,
                        qualityStatus TEXT NOT NULL,
                        usedCount INTEGER NOT NULL,
                        lastUsedAt INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_prepared_questions_bookId ON prepared_questions(bookId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_prepared_questions_chapterId ON prepared_questions(chapterId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_prepared_questions_conceptId ON prepared_questions(conceptId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_prepared_questions_chapterId_qualityStatus ON prepared_questions(chapterId, qualityStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_prepared_questions_fingerprint ON prepared_questions(fingerprint)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS raw_page_text (
                        id TEXT NOT NULL PRIMARY KEY,
                        sourceKey TEXT NOT NULL,
                        pageNumber INTEGER NOT NULL,
                        text TEXT NOT NULL,
                        extractionMethod TEXT NOT NULL,
                        extractionVersion INTEGER NOT NULL,
                        confidence REAL,
                        extractedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_raw_page_text_sourceKey ON raw_page_text(sourceKey)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_raw_page_text_sourceKey_pageNumber ON raw_page_text(sourceKey, pageNumber)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS preparation_jobs (
                        id TEXT NOT NULL PRIMARY KEY,
                        bookId TEXT NOT NULL,
                        chapterId TEXT NOT NULL,
                        status TEXT NOT NULL,
                        progressPercent INTEGER NOT NULL,
                        currentStageGujarati TEXT NOT NULL,
                        errorMessage TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_preparation_jobs_bookId ON preparation_jobs(bookId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_preparation_jobs_chapterId ON preparation_jobs(chapterId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_preparation_jobs_status ON preparation_jobs(status)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS parent_quiz_plans (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        topic TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        skillConceptId TEXT,
                        skillTitleGujarati TEXT,
                        bookId TEXT,
                        bookTitle TEXT,
                        chapterId TEXT,
                        chapterTitleGujarati TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS parent_quiz_questions (
                        id TEXT NOT NULL PRIMARY KEY,
                        planId TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        promptGujarati TEXT NOT NULL,
                        spokenPrompt TEXT NOT NULL,
                        recognitionLanguageTag TEXT NOT NULL,
                        kind TEXT NOT NULL,
                        evaluationMode TEXT NOT NULL,
                        expectedNumber INTEGER,
                        expectedText TEXT,
                        acceptedAnswersJson TEXT NOT NULL,
                        hintGujarati TEXT,
                        correctionGujarati TEXT NOT NULL,
                        sourceLabelsJson TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_parent_quiz_questions_planId ON parent_quiz_questions(planId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_parent_quiz_questions_planId_position ON parent_quiz_questions(planId, position)")

                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS page_knowledge_fts USING FTS4(pageKnowledgeId, bookId, chapterId, pageNumberText, content)"
                )
                db.execSQL(
                    """
                    INSERT INTO page_knowledge_fts(rowid, pageKnowledgeId, bookId, chapterId, pageNumberText, content)
                    SELECT rowid, id, bookId, chapterId, CAST(pageNumber AS TEXT),
                        summaryGujarati || ' ' || COALESCE(visibleTextGujarati, '') || ' ' ||
                        COALESCE(importantObjectsJson, '') || ' ' || COALESCE(exercisesJson, '') || ' ' ||
                        COALESCE(conceptsJson, '')
                    FROM page_knowledge
                    """.trimIndent()
                )
            }
        }

        fun create(context: Context): MitraDatabase =
            Room.databaseBuilder(
                context,
                MitraDatabase::class.java,
                "mitra.db",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
    }
}
