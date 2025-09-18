package com.t1h2h0.videos.data.datasource

import com.t1h2h0.videos.utils.VideoSearchResult
import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor

class VideoDataSourceImpl : VideoDataSource {
    override suspend fun fetchVideoStream(url: String): String {
        var streamUrl: String = ""
        try {
            val extractor =
                YoutubeService(0).getStreamExtractor(url) as YoutubeStreamExtractor
            extractor.fetchPage()

            if (extractor.videoStreams.isNotEmpty()) {
                streamUrl = extractor.videoStreams.first().content ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return streamUrl
    }

    override suspend fun fetchVideoSearch(query: String): List<VideoSearchResult> {
        val results = mutableListOf<VideoSearchResult>()
        return results
    }
}