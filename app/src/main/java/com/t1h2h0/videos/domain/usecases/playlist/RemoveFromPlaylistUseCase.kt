package com.t1h2h0.videos.domain.usecases.playlist

import com.t1h2h0.videos.data.UseCaseResult
import com.t1h2h0.videos.domain.repositories.PlaylistRepository
import com.t1h2h0.videos.domain.usecases.UseCase
import com.t1h2h0.videos.utils.VideoSearchResult
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class RemoveFromPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : UseCase<RemoveFromPlaylistUseCase.Params,UseCaseResult<Unit, Exception>>(
    defaultDispatcher = Dispatchers.IO
) {

    data class Params(val video: VideoSearchResult)

    override suspend fun runInternal(params: Params): UseCaseResult<Unit, Exception> {

        return try {
            playlistRepository.removeFromPlaylist(params.video)
            UseCaseResult.Success(Unit)
        } catch (e: Exception) {
            UseCaseResult.Failure(e)
        }
    }
}