package com.t1h2h0.videos.data.datasource

import com.t1h2h0.videos.utils.VideoSearchResult

interface VideoDataSource {
    suspend fun fetchVideoStream(url: String): String
    suspend fun fetchVideoSearch(query: String): List<VideoSearchResult>

}