package com.t1h2h0.videos.domain.repositories

import com.t1h2h0.videos.data.mappers.HistoryItem
import com.t1h2h0.videos.utils.VideoSearchResult
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getAllHistory(): Flow<List<HistoryItem>>
    suspend fun addToHistory(video: VideoSearchResult)
    suspend fun removeFromHistory(video: VideoSearchResult)
    suspend fun clearHistory()
    suspend fun isInHistory(video: VideoSearchResult): Boolean
}