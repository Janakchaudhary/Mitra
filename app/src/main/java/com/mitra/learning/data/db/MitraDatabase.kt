package com.mitra.learning.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mitra.learning.data.db.dao.BookDao
import com.mitra.learning.data.db.entity.BookEntity

@Database(
    entities = [BookEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class MitraDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        fun create(context: Context): MitraDatabase =
            Room.databaseBuilder(
                context,
                MitraDatabase::class.java,
                "mitra.db",
            ).build()
    }
}
