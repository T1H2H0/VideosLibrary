package com.t1h2h0.videos.domain.repositories

import com.t1h2h0.videos.utils.VideoSearchResult

interface VideoRepository {
    suspend fun getVideoUrl(url: String): String?
    suspend fun searchVideo(query: String, maxResults: Int = 10): List<VideoSearchResult>
    suspend fun searchVideoIds(query: String, maxResults: Int = 10): List<String>
}
