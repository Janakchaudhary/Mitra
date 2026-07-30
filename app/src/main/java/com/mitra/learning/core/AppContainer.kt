package com.mitra.learning.core

import android.content.Context
import com.mitra.learning.books.pdf.AndroidPdfPageRenderer
import com.mitra.learning.data.db.MitraDatabase
import com.mitra.learning.data.repository.BookRepository
import com.mitra.learning.data.repository.LocalBookRepository
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
    val parentPinRepository = ParentPinRepository(appContext)
}
