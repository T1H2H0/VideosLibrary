package com.t1h2h0.videos.domain.usecases
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

abstract class UseCase<in Params, out Result>(
    private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(params: Params): Result {
        return withContext(defaultDispatcher) {
            runInternal(params)
        }
    }

    protected abstract suspend fun runInternal(params: Params): Result
}