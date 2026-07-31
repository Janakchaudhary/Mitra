package com.mitra.learning.core

import android.content.Context
import com.mitra.learning.ai.AiGateway
import com.mitra.learning.ai.MockAiGateway
import com.mitra.learning.books.pdf.AndroidPdfPageRenderer
import com.mitra.learning.data.db.MitraDatabase
import com.mitra.learning.data.repository.BookRepository
import com.mitra.learning.data.repository.LearningRepository
import com.mitra.learning.data.repository.LocalBookRepository
import com.mitra.learning.data.repository.LocalLearningRepository
import com.mitra.learning.learning.engine.DefaultLearningEngine
import com.mitra.learning.learning.engine.LearningEngine
import com.mitra.learning.security.ParentPinRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: MitraDatabase = MitraDatabase.create(appContext)
    val pdfRenderer = AndroidPdfPageRenderer()

    val bookRepository: BookRepository = LocalBookRepository(
        context = appContext,
        bookDao = database.bookDao(),
        pdfRenderer = pdfRenderer,
    )

    val learningRepository: LearningRepository = LocalLearningRepository(
        conceptDao = database.conceptDao(),
        masteryDao = database.masteryDao(),
        sessionDao = database.sessionDao(),
        attemptDao = database.attemptDao(),
    )

    val aiGateway: AiGateway = MockAiGateway()

    val learningEngine: LearningEngine = DefaultLearningEngine(
        repository = learningRepository,
        aiGateway = aiGateway,
    )

    val parentPinRepository = ParentPinRepository(appContext)
}
