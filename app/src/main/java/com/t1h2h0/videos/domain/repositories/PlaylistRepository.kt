package com.t1h2h0.videos.domain.repositories

import com.t1h2h0.videos.data.mappers.PlaylistItem
import com.t1h2h0.videos.utils.VideoSearchResult
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylist(): Flow<List<PlaylistItem>>
    suspend fun addToPlaylist(video: VideoSearchResult)
    suspend fun removeFromPlaylist(video: VideoSearchResult)
    suspend fun clearPlaylist()
    suspend fun isInPlaylist(video: VideoSearchResult): Boolean
    suspend fun movePlaylistItem(fromIndex: Int, toIndex: Int)
}