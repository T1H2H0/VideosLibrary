package com.t1h2h0.videos.di

import android.content.Context
import androidx.room.Room
import com.t1h2h0.videos.data.local.VideoDatabase
import com.t1h2h0.videos.data.local.dao.HistoryDao
import com.t1h2h0.videos.data.local.dao.PlaylistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVideoDatabase(@ApplicationContext context: Context): VideoDatabase {
        return Room.databaseBuilder(
            context,
            VideoDatabase::class.java,
            VideoDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideHistoryDao(database: VideoDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    fun providePlaylistDao(database: VideoDatabase): PlaylistDao {
        return database.playlistDao()
    }
}
