
package com.t1h2h0.videos.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val url: String,
    val duration: Long,
    val publishedTime: String,
    val viewCount: Long,
    val channelTitle: String,
    val addedDate: Date
)