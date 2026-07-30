package com.mitra.learning.books.pdf

import android.graphics.Bitmap

interface PdfPageRenderer {
    suspend fun pageCount(path: String): Int
    suspend fun render(path: String, pageIndex: Int, targetWidthPx: Int): Bitmap
}
