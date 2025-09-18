package com.t1h2h0.videos.utils

import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException

class PipeDownloader(private val client: OkHttpClient) : Downloader() {

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()
        val requestBody = dataToSend?.let { RequestBody.create(null, it) }
        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, requestBody)
            .url(url)
            .addHeader("User-Agent", USER_AGENT)
        headers.forEach { (headerName, headerValueList) ->
            when {
                headerValueList.size > 1 -> {
                    requestBuilder.removeHeader(headerName)
                    headerValueList.forEach { value ->
                        value?.let { requestBuilder.addHeader(headerName, it) }
                    }
                }
                headerValueList.size == 1 -> requestBuilder.header(headerName, headerValueList[0])
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()
        if (response.code == TOO_MANY_REQUESTS) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }
        val responseBodyToReturn = response.body?.string()
        val latestUrl = response.request.url.toString()
        return Response(
            response.code, response.message, response.headers.toMultimap(),
            responseBodyToReturn, latestUrl
        )
    }

    /**
     * Search YT for videos and return video IDs
     * @param query The search query
     * @param maxResults Maximum number of results to return (default: 10)
     * @return List of video IDs
     */
    @Throws(Exception::class)
    fun searchVideos(query: String, maxResults: Int = 10): List<String> {
        // Initialize NewPipe with this downloader
        NewPipe.init(this)

        val video = ServiceList.YouTube
        val searchExtractor = video.getSearchExtractor(query)

        // Fetch initial page
        searchExtractor.fetchPage()

        val videoIds = mutableListOf<String>()
        val items = searchExtractor.initialPage.items

        var count = 0
        for (item in items) {
            if (count >= maxResults) break

            // Only process video items, skip channels, playlists, etc.
            if (item is StreamInfoItem) {
                val videoUrl = item.url
                // Extract video ID from URL
                val videoId = extractVideoIdFromUrl(videoUrl)
                if (videoId != null) {
                    videoIds.add(videoId)
                    count++
                }
            }
        }

        return videoIds
    }

    /**
     * Search YT for videos and return detailed results
     * @param query The search query
     * @param maxResults Maximum number of results to return (default: 10)
     * @return List of VideoSearchResult objects
     */
    @Throws(Exception::class)
    fun searchVideosDetailed(query: String, maxResults: Int = 10): List<VideoSearchResult> {
        // Initialize NewPipe with this downloader
        NewPipe.init(this)

        val video = ServiceList.YouTube
        val searchExtractor = video.getSearchExtractor(query)

        // Fetch initial page
        searchExtractor.fetchPage()

        val results = mutableListOf<VideoSearchResult>()
        val items = searchExtractor.initialPage.items

        var count = 0
        for (item in items) {
            if (count >= maxResults) break

            // Only process video items and cast to StreamInfoItem
            if (item is StreamInfoItem) {
                val videoUrl = item.url
                val videoId = extractVideoIdFromUrl(videoUrl)
                if (videoId != null) {
                    results.add(
                        VideoSearchResult(
                            id = videoId,
                            title = item.name ?: "Unknown Title",
                            url = videoUrl,
                            uploaderName = item.uploaderName ?: "Unknown",
                            channelName = item.uploaderName ?: "Unknown",
                            thumbnailUrl = item.thumbnails.firstOrNull()?.url,
                            duration = item.duration,
                            viewCount = item.viewCount,
                            publishedTime ="",
                            description = "",
                            tags = listOf(""),
                            channelTitle = ""
                        )
                    )
                    count++
                }
            }
        }

        return results
    }
    /**
     * Extract video ID from YT URL
     * @param url YT video URL
     * @return Video ID or null if not found
     */
    private fun extractVideoIdFromUrl(url: String): String? {
        val patterns = listOf(
            "(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([a-zA-Z0-9_-]{11})",
            "youtube\\.com/watch\\?.*v=([a-zA-Z0-9_-]{11})"
        )

        for (pattern in patterns) {
            val regex = pattern.toRegex()
            val match = regex.find(url)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:68.0) Gecko/20100101 Firefox/68.0"

        private const val TOO_MANY_REQUESTS = 429

        fun create(client: OkHttpClient): PipeDownloader {
            return PipeDownloader(client)
        }
    }
}

/**
 * Data class to hold video search results
 */
data class VideoSearchResult(
    val id: String,
    val title: String,
    val channelName: String,
    val url: String,
    val uploaderName: String,
    val thumbnailUrl: String?,
    val duration: Long=0,
    val viewCount: Long=0,
    val publishedTime: String?,
    val description: String?,
    val tags: List<String>,
    val channelTitle: String?
)