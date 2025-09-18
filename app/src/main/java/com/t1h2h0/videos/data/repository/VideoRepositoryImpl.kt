package com.t1h2h0.videos.data.repository

import com.t1h2h0.videos.data.datasource.VideoDataSource
import com.t1h2h0.videos.domain.repositories.VideoRepository
import com.t1h2h0.videos.utils.PipeDownloader
import com.t1h2h0.videos.utils.VideoSearchResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl @Inject constructor(
    private val videoDataSource: VideoDataSource,
    private val pipeDownloader: PipeDownloader
): VideoRepository {
    override suspend fun getVideoUrl(url: String): String? {
        val streamUrl = videoDataSource.fetchVideoStream(url)
        return streamUrl.ifEmpty {
            null
        }
    }
    override suspend fun searchVideo(query: String, maxResults: Int): List<VideoSearchResult> {
        return pipeDownloader.searchVideosDetailed(query, maxResults)
    }

    override suspend fun searchVideoIds(query: String, maxResults: Int): List<String> {
        return pipeDownloader.searchVideos(query, maxResults)
    }
}