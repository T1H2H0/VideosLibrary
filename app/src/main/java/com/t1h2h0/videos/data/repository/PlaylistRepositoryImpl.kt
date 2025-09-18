

package com.t1h2h0.videos.data.repositories

import com.t1h2h0.videos.data.local.dao.PlaylistDao
import com.t1h2h0.videos.data.mappers.PlaylistItem
import com.t1h2h0.videos.data.mappers.toPlaylistEntity
import com.t1h2h0.videos.data.mappers.toPlaylistItem
import com.t1h2h0.videos.domain.repositories.PlaylistRepository
import com.t1h2h0.videos.utils.VideoSearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    override fun getAllPlaylist(): Flow<List<PlaylistItem>> {
        return playlistDao.getAllPlaylist().map { entities ->
            entities.map { it.toPlaylistItem() } as List<PlaylistItem>
        }
    }

    override suspend fun addToPlaylist(video: VideoSearchResult) {
        // Check if video already exists
        val existingVideo = playlistDao.getPlaylistById(video.id)
        if (existingVideo != null) return

        // Get next position
        val maxPosition = playlistDao.getMaxPosition() ?: -1
        val entity = video.toPlaylistEntity(Date(), maxPosition + 1)
        playlistDao.insertPlaylist(entity)
    }

    override suspend fun removeFromPlaylist(video: VideoSearchResult) {
        val entity = playlistDao.getPlaylistById(video.id)
        entity?.let { playlistDao.deletePlaylist(it) }
    }

    override suspend fun clearPlaylist() {
        playlistDao.clearPlaylist()
    }

    override suspend fun isInPlaylist(video: VideoSearchResult): Boolean {
        return playlistDao.getPlaylistById(video.id) != null
    }

    override suspend fun movePlaylistItem(fromIndex: Int, toIndex: Int) {
        // This is a simplified implementation
        // You might need a more sophisticated approach for reordering
        val allItems = playlistDao.getAllPlaylist()
        // Implementation depends on your specific reordering logic
    }
}