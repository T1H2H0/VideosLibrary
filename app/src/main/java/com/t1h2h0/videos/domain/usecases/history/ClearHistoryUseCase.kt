package com.t1h2h0.videos.domain.usecases.history

import com.t1h2h0.videos.data.UseCaseResult
import com.t1h2h0.videos.domain.repositories.HistoryRepository
import com.t1h2h0.videos.domain.usecases.UseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class ClearHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) : UseCase<Unit,  UseCaseResult<Unit, Exception>>(
    defaultDispatcher = Dispatchers.IO
){
    override suspend fun runInternal(params: Unit): UseCaseResult<Unit, Exception> {
        return try {
            historyRepository.clearHistory()
            UseCaseResult.Success(Unit)
        } catch (e: Exception) {
            UseCaseResult.Failure(e)
        }
    }
}
