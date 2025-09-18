package com.t1h2h0.videos.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "playlist")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val url: String,
    val duration: Long=0,
    val publishedTime: String,
    val viewCount: Long=0,
    val channelTitle: String,
    val channelId: String,
    val uploaderName: String,
    val addedDate: Date,
    val position: Int = 0
)