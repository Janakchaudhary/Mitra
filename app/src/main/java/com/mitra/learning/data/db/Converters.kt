package com.mitra.learning.data.db

import androidx.room.TypeConverter
import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.data.db.entity.BookAnalysisStatus
import com.mitra.learning.data.db.entity.ChapterAnalysisStatus
import com.mitra.learning.data.db.entity.SessionStatus

class Converters {
    @TypeConverter
    fun fromAnalysisStatus(value: BookAnalysisStatus): String = value.name

    @TypeConverter
    fun toAnalysisStatus(value: String): BookAnalysisStatus = BookAnalysisStatus.valueOf(value)

    @TypeConverter
    fun fromChapterAnalysisStatus(value: ChapterAnalysisStatus): String = value.name

    @TypeConverter
    fun toChapterAnalysisStatus(value: String): ChapterAnalysisStatus = ChapterAnalysisStatus.valueOf(value)

    @TypeConverter
    fun fromSessionStatus(value: SessionStatus): String = value.name

    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus = SessionStatus.valueOf(value)

    @TypeConverter
    fun fromAttemptResult(value: AttemptResult): String = value.name

    @TypeConverter
    fun toAttemptResult(value: String): AttemptResult = AttemptResult.valueOf(value)
}
