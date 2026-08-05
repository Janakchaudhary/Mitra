package com.mitra.learning.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mitra.learning.data.db.entity.RawPageTextEntity

@Dao
interface RawPageTextDao {
    @Query("SELECT * FROM raw_page_text WHERE sourceKey = :sourceKey AND pageNumber IN (:pageNumbers)")
    suspend fun find(sourceKey: String, pageNumbers: List<Int>): List<RawPageTextEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RawPageTextEntity>)

    @Query("DELETE FROM raw_page_text WHERE sourceKey = :sourceKey")
    suspend fun deleteForSource(sourceKey: String)
}
