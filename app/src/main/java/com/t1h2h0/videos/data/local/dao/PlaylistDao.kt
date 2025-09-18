package com.t1h2h0.videos.data.local.dao

import androidx.room.*
import com.t1h2h0.videos.data.local.entities.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlist ORDER BY position ASC")
    fun getAllPlaylist(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist WHERE id = :id")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlist")
    suspend fun clearPlaylist()

    @Query("SELECT COUNT(*) FROM playlist")
    suspend fun getPlaylistCount(): Int

    @Query("UPDATE playlist SET position = :position WHERE id = :id")
    suspend fun updatePlaylistPosition(id: String, position: Int)

    @Query("SELECT MAX(position) FROM playlist")
    suspend fun getMaxPosition(): Int?
}