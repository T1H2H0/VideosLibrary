package com.t1h2h0.videos.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ThumbnailCache {
    private val cache = mutableMapOf<String, ByteArray>()

    fun getThumbnailByteArray(
        thumbnailUrl: String?,
        callback: (ByteArray?) -> Unit
    ) {
        if (thumbnailUrl.isNullOrEmpty()) {
            callback(null)
            return
        }

        // Check cache first
        cache[thumbnailUrl]?.let {
            callback(it)
            return
        }

        // Download and cache asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            val byteArray = getThumbnailByteArrayWithOkHttp(thumbnailUrl)
            byteArray?.let { cache[thumbnailUrl] = it }
            callback(byteArray)
        }
    }

    fun clearCache() {
        cache.clear()
    }
}