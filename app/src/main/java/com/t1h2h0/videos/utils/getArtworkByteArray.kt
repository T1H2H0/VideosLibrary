package com.t1h2h0.videos.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream

suspend fun getThumbnailByteArrayWithOkHttp(thumbnailUrl: String?): ByteArray? {
    return withContext(Dispatchers.IO) {
        try {
            if (thumbnailUrl.isNullOrEmpty()) return@withContext null

            val client = OkHttpClient()
            val request = Request.Builder()
                .url(thumbnailUrl)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val bytes = response.body?.bytes()
            response.close()

            // Optionally process the image to ensure proper format/size
            bytes?.let { imageBytes ->
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                val resizedBitmap = if (bitmap.width > 512 || bitmap.height > 512) {
                    Bitmap.createScaledBitmap(bitmap, 512, 512, true)
                } else {
                    bitmap
                }

                val byteArrayOutputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
                val finalByteArray = byteArrayOutputStream.toByteArray()

                byteArrayOutputStream.close()
                if (resizedBitmap != bitmap) {
                    resizedBitmap.recycle()
                }
                bitmap.recycle()

                finalByteArray
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}