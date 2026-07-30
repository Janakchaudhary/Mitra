package com.mitra.learning.books.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidPdfPageRenderer : PdfPageRenderer {
    override suspend fun pageCount(path: String): Int = withContext(Dispatchers.IO) {
        openRenderer(path).use { holder -> holder.renderer.pageCount }
    }

    override suspend fun render(
        path: String,
        pageIndex: Int,
        targetWidthPx: Int,
    ): Bitmap = withContext(Dispatchers.IO) {
        require(targetWidthPx > 0)
        openRenderer(path).use { holder ->
            require(pageIndex in 0 until holder.renderer.pageCount) {
                "Page $pageIndex outside PDF range"
            }
            holder.renderer.openPage(pageIndex).use { page ->
                val width = targetWidthPx
                val height = ((page.height.toFloat() / page.width.toFloat()) * width)
                    .toInt()
                    .coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    }

    private fun openRenderer(path: String): RendererHolder {
        val pfd = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
        return RendererHolder(pfd, PdfRenderer(pfd))
    }

    private class RendererHolder(
        private val pfd: ParcelFileDescriptor,
        val renderer: PdfRenderer,
    ) : AutoCloseable {
        override fun close() {
            renderer.close()
            pfd.close()
        }
    }
}
