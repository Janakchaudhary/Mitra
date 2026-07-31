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
import com.mitra.learning.data.db.dao.SessionDao
import com.mitra.learning.data.db.entity.AttemptEntity
import com.mitra.learning.data.db.entity.BookEntity
import com.mitra.learning.data.db.entity.ChapterEntity
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.ConceptPrerequisiteEntity
import com.mitra.learning.data.db.entity.MasteryEntity
import com.mitra.learning.data.db.entity.PageKnowledgeEntity
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
    ],
    version = 3,
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

        fun create(context: Context): MitraDatabase =
            Room.databaseBuilder(
                context,
                MitraDatabase::class.java,
                "mitra.db",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
