package com.t1h2h0.videos.data.repositories

import com.t1h2h0.videos.data.local.dao.HistoryDao
import com.t1h2h0.videos.data.mappers.HistoryItem
import com.t1h2h0.videos.data.mappers.toHistoryEntity
import com.t1h2h0.videos.data.mappers.toHistoryItem
import com.t1h2h0.videos.domain.repositories.HistoryRepository
import com.t1h2h0.videos.utils.VideoSearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryRepository {

    override fun getAllHistory(): Flow<List<HistoryItem>> {
        return historyDao.getAllHistory().map { entities ->
            entities.map { it.toHistoryItem() } as List<HistoryItem>
        }
    }

    override suspend fun addToHistory(video: VideoSearchResult) {
        val entity = video.toHistoryEntity(Date())
        historyDao.insertHistory(entity)

        // Keep only last 100 items
        val count = historyDao.getHistoryCount()
        if (count > 100) {
            historyDao.limitHistoryTo100()
        }
    }

    override suspend fun removeFromHistory(video: VideoSearchResult) {
        val entity = historyDao.getHistoryById(video.id)
        entity?.let { historyDao.deleteHistory(it) }
    }

    override suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    override suspend fun isInHistory(video: VideoSearchResult): Boolean {
        return historyDao.getHistoryById(video.id) != null
    }
}