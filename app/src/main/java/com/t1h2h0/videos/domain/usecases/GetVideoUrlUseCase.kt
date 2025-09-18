package com.t1h2h0.videos.domain.usecases

import kotlinx.coroutines.CoroutineDispatcher
import com.t1h2h0.videos.data.UseCaseResult
import com.t1h2h0.videos.domain.repositories.VideoRepository
import com.t1h2h0.videos.di.IODispatcher
import javax.inject.Inject

class GetVideoUrlUseCase @Inject constructor(
    private val videoRepository: VideoRepository,
    @IODispatcher defaultDispatcher: CoroutineDispatcher
) : UseCase<GetVideoUrlUseCase.Params, UseCaseResult<String, Exception>>(defaultDispatcher) {

    override suspend fun runInternal(params: Params): UseCaseResult<String, Exception> {
        return try {
            val url = videoRepository.getVideoUrl(params.videoUrl)
            if (url != null) {
                UseCaseResult.Success(url)
            } else {
                UseCaseResult.Failure(Exception("Unable to get URL"))
            }
        } catch (e: Exception) {
            UseCaseResult.Failure(e)
        }
    }

    data class Params(
        val videoUrl: String
    )


}

