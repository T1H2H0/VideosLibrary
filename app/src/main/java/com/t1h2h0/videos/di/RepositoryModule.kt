package com.t1h2h0.videos.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.t1h2h0.videos.data.datasource.VideoDataSource
import com.t1h2h0.videos.data.datasource.VideoDataSourceImpl
import com.t1h2h0.videos.data.repositories.HistoryRepositoryImpl
import com.t1h2h0.videos.data.repositories.PlaylistRepositoryImpl
import com.t1h2h0.videos.domain.repositories.HistoryRepository
import com.t1h2h0.videos.domain.repositories.VideoRepository
import com.t1h2h0.videos.data.repository.VideoRepositoryImpl
import com.t1h2h0.videos.domain.repositories.PlaylistRepository
import com.t1h2h0.videos.utils.PipeDownloader
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideVideoDataSource(): VideoDataSource {
        return VideoDataSourceImpl()
    }

    @Provides
    @Singleton
    fun provideVideoRepository(pipeDownloader: PipeDownloader, videoDataSource: VideoDataSource): VideoRepository {
        return VideoRepositoryImpl(videoDataSource, pipeDownloader)
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(
        historyRepositoryImpl: HistoryRepositoryImpl
    ): HistoryRepository {
        return historyRepositoryImpl
    }

    @Provides
    @Singleton
    fun providePlaylistRepository(
        playlistRepositoryImpl: PlaylistRepositoryImpl
    ): PlaylistRepository {
        return playlistRepositoryImpl
    }
}