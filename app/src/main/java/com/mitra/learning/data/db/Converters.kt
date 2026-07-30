package com.mitra.learning.data.db

import androidx.room.TypeConverter
import com.mitra.learning.data.db.entity.BookAnalysisStatus

class Converters {
    @TypeConverter
    fun fromAnalysisStatus(value: BookAnalysisStatus): String = value.name

    @TypeConverter
    fun toAnalysisStatus(value: String): BookAnalysisStatus = BookAnalysisStatus.valueOf(value)
}
