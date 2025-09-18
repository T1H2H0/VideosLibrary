package com.t1h2h0.videos.data.mappers

import com.t1h2h0.videos.data.local.entities.HistoryEntity
import com.t1h2h0.videos.utils.VideoSearchResult
import java.util.Date

fun VideoSearchResult.toHistoryEntity(addedDate: Date): HistoryEntity {
    return HistoryEntity(
        id = this.id,
        title = this.title,
        description = this.description.toString(),
        thumbnailUrl = this.thumbnailUrl.toString(),
        url = this.url,
        duration = this.duration,
        publishedTime = this.publishedTime.toString(),
        viewCount = this.viewCount,
        channelTitle = this.channelTitle.toString(),
        addedDate = addedDate
    )
}

fun HistoryEntity.toVideoSearchResult(): VideoSearchResult {
    return VideoSearchResult(
        id = this.id,
        title = this.title,
        description = this.description,
        thumbnailUrl = this.thumbnailUrl,
        url = this.url,
        duration = this.duration,
        publishedTime = this.publishedTime,
        viewCount = this.viewCount,
        channelTitle = this.channelTitle,
        channelName = "",
        uploaderName = "",
        tags = listOf()
    )
}

fun HistoryEntity.toHistoryItem(): HistoryItem {
    return HistoryItem(
        video = this.toVideoSearchResult(),
        watchedAt = this.addedDate,
        date = this.addedDate,

    )
}

data class HistoryItem (val video: VideoSearchResult,   val watchedAt: Date,
                        val date: Date)


