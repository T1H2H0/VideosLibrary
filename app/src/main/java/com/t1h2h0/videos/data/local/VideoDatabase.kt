
package com.t1h2h0.videos.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.t1h2h0.videos.data.local.converters.DateConverter
import com.t1h2h0.videos.data.local.dao.HistoryDao
import com.t1h2h0.videos.data.local.dao.PlaylistDao
import com.t1h2h0.videos.data.local.entities.HistoryEntity
import com.t1h2h0.videos.data.local.entities.PlaylistEntity

@Database(
    entities = [HistoryEntity::class, PlaylistEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class VideoDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        const val DATABASE_NAME = "video_database"
    }
}