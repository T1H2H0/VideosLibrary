package com.t1h2h0.videos.domain.usecases.history

import com.t1h2h0.videos.data.mappers.HistoryItem
import com.t1h2h0.videos.domain.repositories.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    operator fun invoke(): Flow<List<HistoryItem>> =
        historyRepository.getAllHistory()
}
