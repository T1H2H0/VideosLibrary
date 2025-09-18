package com.t1h2h0.videos.data.mappers

import com.t1h2h0.videos.data.local.entities.PlaylistEntity
import com.t1h2h0.videos.utils.VideoSearchResult
import java.util.Date

fun VideoSearchResult.toPlaylistEntity(addedDate: Date, position: Int): PlaylistEntity {
    return PlaylistEntity(
        id = this.id,
        title = this.title,
        description = this.description.toString(),
        thumbnailUrl = this.thumbnailUrl.toString(),
        url = this.url,
        duration = this.duration,
        publishedTime = this.publishedTime.toString(),
        viewCount = this.viewCount,
        channelTitle = this.channelTitle.toString(),
        addedDate = addedDate,
        position = position,
        channelId ="",
        uploaderName = ""
    )
}

fun PlaylistEntity.toVideoSearchResult(): VideoSearchResult {
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
        uploaderName = this.uploaderName,
        tags = listOf()
    )
}

fun PlaylistEntity.toPlaylistItem(): PlaylistItem {
    return PlaylistItem(
        video = this.toVideoSearchResult(),
        date = this.addedDate
    )
}

data class PlaylistItem (val video: VideoSearchResult, val date: Date)


