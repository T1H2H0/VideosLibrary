package com.t1h2h0.videos.domain.usecases.playlist

import com.t1h2h0.videos.data.UseCaseResult
import com.t1h2h0.videos.domain.repositories.PlaylistRepository
import com.t1h2h0.videos.domain.usecases.UseCase
import com.t1h2h0.videos.domain.usecases.playlist.AddToPlaylistUseCase.Params
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class ClearPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : UseCase<Unit ,UseCaseResult<Unit, Exception>>(
    defaultDispatcher = Dispatchers.IO
){

    override suspend fun runInternal(params: Unit): UseCaseResult<Unit, Exception> {

        return try {
            playlistRepository.clearPlaylist()
            UseCaseResult.Success(Unit)
        } catch (e: Exception) {
            UseCaseResult.Failure(e)
        }
    }
}