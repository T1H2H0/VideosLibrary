package com.t1h2h0.videos.domain.usecases

import kotlinx.coroutines.CoroutineDispatcher
import com.t1h2h0.videos.data.UseCaseResult
import com.t1h2h0.videos.domain.repositories.VideoRepository
import com.t1h2h0.videos.di.IODispatcher
import com.t1h2h0.videos.utils.VideoSearchResult
import javax.inject.Inject

class SearchVideoUseCase @Inject constructor(
    private val videoRepository: VideoRepository,
    @IODispatcher defaultDispatcher: CoroutineDispatcher
) : UseCase<SearchVideoUseCase.Params, UseCaseResult<List<VideoSearchResult>, Exception>>(defaultDispatcher) {

    override suspend fun runInternal(params: Params): UseCaseResult<List<VideoSearchResult>, Exception> {
        return try {
            val results = videoRepository.searchVideo(params.query, params.maxResults)
            UseCaseResult.Success(results)
        } catch (e: Exception) {
            UseCaseResult.Failure(e)
        }
    }

    data class Params(
        val query: String,
        val maxResults: Int = 10
    )
}

class SearchVideoIdsUseCase @Inject constructor(
    private val videoRepository: VideoRepository,
    @IODispatcher defaultDispatcher: CoroutineDispatcher
) : UseCase<SearchVideoIdsUseCase.Params, UseCaseResult<List<String>, Exception>>(defaultDispatcher) {

    override suspend fun runInternal(params: Params): UseCaseResult<List<String>, Exception> {
        return try {
            val videoIds = videoRepository.searchVideoIds(params.query, params.maxResults)
            UseCaseResult.Success(videoIds)
        } catch (e: Exception) {
            UseCaseResult.Failure(e)
        }
    }

    data class Params(
        val query: String,
        val maxResults: Int = 10
    )
}