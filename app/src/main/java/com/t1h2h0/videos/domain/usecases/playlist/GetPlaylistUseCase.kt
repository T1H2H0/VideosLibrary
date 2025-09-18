package com.t1h2h0.videos.domain.usecases.playlist

import com.t1h2h0.videos.data.mappers.PlaylistItem
import com.t1h2h0.videos.domain.repositories.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    operator fun invoke(): Flow<List<PlaylistItem>> {
        return playlistRepository.getAllPlaylist()
    }
}