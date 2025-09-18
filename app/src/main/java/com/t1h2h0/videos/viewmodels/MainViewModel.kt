package com.t1h2h0.videos.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.t1h2h0.videos.data.UseCaseResult
import com.t1h2h0.videos.data.mappers.HistoryItem
import com.t1h2h0.videos.data.mappers.PlaylistItem
import com.t1h2h0.videos.domain.usecases.GetVideoUrlUseCase
import com.t1h2h0.videos.domain.usecases.SearchVideoIdsUseCase
import com.t1h2h0.videos.domain.usecases.SearchVideoUseCase
import com.t1h2h0.videos.domain.usecases.history.AddToHistoryUseCase
import com.t1h2h0.videos.domain.usecases.history.ClearHistoryUseCase
import com.t1h2h0.videos.domain.usecases.history.GetHistoryUseCase
import com.t1h2h0.videos.domain.usecases.history.RemoveFromHistoryUseCase
import com.t1h2h0.videos.domain.usecases.playlist.AddToPlaylistUseCase
import com.t1h2h0.videos.domain.usecases.playlist.ClearPlaylistUseCase
import com.t1h2h0.videos.domain.usecases.playlist.GetPlaylistUseCase
import com.t1h2h0.videos.domain.usecases.playlist.RemoveFromPlaylistUseCase
import com.t1h2h0.videos.domain.repositories.HistoryRepository
import com.t1h2h0.videos.domain.repositories.PlaylistRepository
import com.t1h2h0.videos.utils.VideoSearchResult
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getVideoUrlUseCase: GetVideoUrlUseCase,
    private val searchvideoUseCase: SearchVideoUseCase,
    private val searchVideoIdsUseCase: SearchVideoIdsUseCase,
    private val addToHistoryUseCase: AddToHistoryUseCase,
    private val removeFromHistoryUseCase: RemoveFromHistoryUseCase,
    private val clearHistoryUseCase: ClearHistoryUseCase,
    private val getHistoryUseCase: GetHistoryUseCase,
    private val addToPlaylistUseCase: AddToPlaylistUseCase,
    private val removeFromPlaylistUseCase: RemoveFromPlaylistUseCase,
    private val clearPlaylistUseCase: ClearPlaylistUseCase,
    private val getPlaylistUseCase: GetPlaylistUseCase,
    private val historyRepository: HistoryRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _videoUrlState = MutableStateFlow<Result<String>>(Result.Loading)
    val videoUrlState: StateFlow<Result<String>> = _videoUrlState.asStateFlow()

    private val _searchResultsState = MutableStateFlow<Result<List<VideoSearchResult>>>(Result.Idle)
    val searchResultsState: StateFlow<Result<List<VideoSearchResult>>> = _searchResultsState.asStateFlow()
    private val _searchPredictionsState = MutableStateFlow<Result<List<String>>>(Result.Idle)
    val searchPredictionsState: StateFlow<Result<List<String>>> = _searchPredictionsState.asStateFlow()

    val currentVideoUrl = MutableStateFlow<String?>(null)

    private val _searchIdsState = MutableStateFlow<Result<List<String>>>(Result.Loading)
    val searchIdsState: StateFlow<Result<List<String>>> = _searchIdsState.asStateFlow()

    // History management - now using Room database
    private val _historyItems = getHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val historyItems: StateFlow<List<HistoryItem>> = _historyItems

    private val _playlistItems = getPlaylistUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playlistItems: StateFlow<List<PlaylistItem>> = _playlistItems
    // Current video info for sharing between screens
    private val _currentVideoInfo = MutableStateFlow<VideoSearchResult?>(null)
    val currentVideoInfo: StateFlow<VideoSearchResult?> = _currentVideoInfo.asStateFlow()

    // Legacy support for existing code
    private val _playbackHistory = MutableStateFlow<List<VideoSearchResult>>(emptyList())
    val playbackHistory: StateFlow<List<VideoSearchResult>> = _playbackHistory.asStateFlow()

    private val _playlist = MutableStateFlow<List<VideoSearchResult>>(emptyList())
    val playlist: StateFlow<List<VideoSearchResult>> = _playlist.asStateFlow()

    private val _currentPlaylistIndex = MutableStateFlow<Int>(-1)
    val currentPlaylistIndex: StateFlow<Int> = _currentPlaylistIndex.asStateFlow()

    // Playback state to survive rotation
    private val _playbackPosition = MutableStateFlow<Long>(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _isPlaying = MutableStateFlow<Boolean>(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    val mediaController = MutableStateFlow<MediaController?>(null)

    fun setCurrentVideo(videoInfo: VideoSearchResult, videoUrl: String) {
        _currentVideoInfo.value = videoInfo
        currentVideoUrl.value = videoUrl
        addToHistory(videoInfo)
    }

    fun getCurrentVideoInfo(): VideoSearchResult? = _currentVideoInfo.value

    fun updatePlaybackState(position: Long, isPlaying: Boolean) {
        _playbackPosition.value = position
        _isPlaying.value = isPlaying
    }

    // History Management - using Room database
    fun addToHistory(video: VideoSearchResult) {
        viewModelScope.launch {
            try {
                addToHistoryUseCase(AddToHistoryUseCase.Params(video))
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to add to history", e)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                clearHistoryUseCase(Unit)
                _playbackHistory.value = emptyList()
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to clear history", e)
            }
        }
    }

    fun removeFromHistory(video: VideoSearchResult) {
        viewModelScope.launch {
            try {
                removeFromHistoryUseCase(RemoveFromHistoryUseCase.Params(video))
                _playbackHistory.value = _playbackHistory.value.filter { it.id != video.id }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to remove from history", e)
            }
        }
    }

    fun removeFromHistory(historyItem: HistoryItem) {
        removeFromHistory(historyItem.video)
    }

    // Playlist Management - using Room database
    fun addToPlaylist(video: VideoSearchResult) {
        viewModelScope.launch {
            try {
                addToPlaylistUseCase(AddToPlaylistUseCase.Params(video))
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to add to playlist", e)
            }
        }
    }

    fun removeFromPlaylist(video: VideoSearchResult) {
        viewModelScope.launch {
            try {
                removeFromPlaylistUseCase(RemoveFromPlaylistUseCase.Params(video))

                // Update legacy playlist state
                val currentPlaylist = _playlist.value.toMutableList()
                val removedIndex = currentPlaylist.indexOfFirst { it.id == video.id }

                if (removedIndex != -1) {
                    currentPlaylist.removeAt(removedIndex)
                    _playlist.value = currentPlaylist

                    // Adjust current index if needed
                    val currentIndex = _currentPlaylistIndex.value
                    if (currentIndex > removedIndex) {
                        _currentPlaylistIndex.value = currentIndex - 1
                    } else if (currentIndex == removedIndex) {
                        _currentPlaylistIndex.value = -1
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to remove from playlist", e)
            }
        }
    }

    fun removeFromPlaylist(playlistItem: PlaylistItem) {
        removeFromPlaylist(playlistItem.video)
    }

    fun clearPlaylist() {
        viewModelScope.launch {
            try {
                clearPlaylistUseCase(Unit)
                _playlist.value = emptyList()
                _currentPlaylistIndex.value = -1
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to clear playlist", e)
            }
        }
    }

    fun playFromPlaylist(index: Int) {
        val playlist = _playlist.value
        if (index >= 0 && index < playlist.size) {
            _currentPlaylistIndex.value = index
            val video = playlist[index]
            getVideoUrlFromVideoResult(video)
        }
    }
// Add this method to your MainViewModel class

    fun getSearchPredictions(query: String) {
        viewModelScope.launch {
            _searchPredictionsState.value = Result.Loading
            try {
                val predictions = getVideoSearchSuggestions(query)
                _searchPredictionsState.value = Result.Success(predictions)
            } catch (e: Exception) {
                _searchPredictionsState.value = Result.Error(e)
            }
        }
    }

    // Add this private method to fetch  search suggestions
    private suspend fun getVideoSearchSuggestions(query: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Using 's suggestion API
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val url = "http://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q=$encodedQuery"

                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                val suggestions = mutableListOf<String>()

                responseBody?.let { body ->
                    val jsonStart = body.indexOf("[[")
                    val jsonEnd = body.lastIndexOf("]]")

                    if (jsonStart != -1 && jsonEnd != -1) {
                        val jsonContent = body.substring(jsonStart, jsonEnd + 2)

                        // Extract suggestions using regex
                        val suggestionRegex = "\\[\"([^\"]+)\"".toRegex()
                        val matches = suggestionRegex.findAll(jsonContent)

                        matches.forEach { match ->
                            val suggestion = match.groupValues[1]
                            if (suggestion.isNotBlank() && suggestion != query) {
                                suggestions.add(suggestion)
                            }
                        }
                    }
                }

                suggestions.take(5) // Return max 5 suggestions
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error fetching suggestions", e)
                // Fallback to local suggestions if network fails
                getLocalSuggestions(query)
            }
        }
    }

    // Fallback method for local suggestions when network fails
    private fun getLocalSuggestions(query: String): List<String> {
        val suggestions = mutableListOf<String>()

        when {
            query.lowercase().contains("music") -> {
                suggestions.addAll(listOf(
                    "$query playlist",
                    "$query live",
                    "$query remix",
                    "$query cover",
                    "$query instrumental"
                ))
            }
            query.lowercase().contains("tutorial") -> {
                suggestions.addAll(listOf(
                    "$query for beginners",
                    "$query step by step",
                    "$query guide",
                    "$query tips and tricks"
                ))
            }
            query.lowercase().contains("review") -> {
                suggestions.addAll(listOf(
                    "$query 2024",
                    "$query honest review",
                    "$query vs",
                    "$query comparison"
                ))
            }
            query.lowercase().contains("game") || query.lowercase().contains("gaming") -> {
                suggestions.addAll(listOf(
                    "$query gameplay",
                    "$query walkthrough",
                    "$query tips",
                    "$query guide",
                    "$query review"
                ))
            }
            query.lowercase().contains("movie") || query.lowercase().contains("film") -> {
                suggestions.addAll(listOf(
                    "$query trailer",
                    "$query review",
                    "$query explained",
                    "$query behind scenes",
                    "$query analysis"
                ))
            }
            else -> {
                suggestions.addAll(listOf(
                    "$query tutorial",
                    "$query review",
                    "$query guide",
                    "$query tips",
                    "$query 2024"
                ))
            }
        }

        return suggestions.take(5)
    }

    // Optional: Clear predictions when needed
    fun clearPredictions() {
        _searchPredictionsState.value = Result.Idle
    }
    fun playNext() {
        val playlist = _playlist.value
        val currentIndex = _currentPlaylistIndex.value

        if (playlist.isNotEmpty() && currentIndex < playlist.size - 1) {
            playFromPlaylist(currentIndex + 1)
        }
    }

    fun playPrevious() {
        val currentIndex = _currentPlaylistIndex.value

        if (currentIndex > 0) {
            playFromPlaylist(currentIndex - 1)
        }
    }

    fun movePlaylistItem(fromIndex: Int, toIndex: Int) {
        val currentPlaylist = _playlist.value.toMutableList()

        if (fromIndex >= 0 && fromIndex < currentPlaylist.size &&
            toIndex >= 0 && toIndex < currentPlaylist.size) {

            val item = currentPlaylist.removeAt(fromIndex)
            currentPlaylist.add(toIndex, item)
            _playlist.value = currentPlaylist

            // Adjust current index if needed
            val currentIndex = _currentPlaylistIndex.value
            when {
                currentIndex == fromIndex -> _currentPlaylistIndex.value = toIndex
                currentIndex in (minOf(fromIndex, toIndex) until maxOf(fromIndex, toIndex)) -> {
                    if (fromIndex < toIndex) {
                        _currentPlaylistIndex.value = currentIndex - 1
                    } else {
                        _currentPlaylistIndex.value = currentIndex + 1
                    }
                }
            }
        }
    }

    // Helper function to get Video URL from VideoSearchResult
    private fun getVideoUrlFromVideoResult(video: VideoSearchResult) {
        val videoUrl = "https://www.youtube.com/watch?v=${video.id}"
        getVideoUrl(videoUrl)
    }

    fun getVideoUrl(url: String) {
        viewModelScope.launch {
            val result = getVideoUrlUseCase(GetVideoUrlUseCase.Params(url))
            _videoUrlState.value = when (result) {
                is UseCaseResult.Success -> {
                    Result.Success(result.data ?: "")
                }
                is UseCaseResult.Failure -> {
                    Result.Error(result.error)
                }
                else -> {
                    Result.Error(Exception("Unknown error"))
                }
            }
        }
    }

    fun searchVideos(query: String, maxResults: Int = 10) {
        viewModelScope.launch {
            _searchResultsState.value = Result.Loading

            val searchParams = SearchVideoUseCase.Params(
                query = query,
                maxResults = maxResults
            )

            val result = searchvideoUseCase(searchParams)
            _searchResultsState.value = when (result) {
                is UseCaseResult.Success -> {
                    Result.Success(result.data)
                }
                is UseCaseResult.Failure -> {
                    Result.Error(result.error)
                }
                else -> {
                    Result.Error(Exception("Unknown error"))
                }
            }
        }
    }

    fun searchVideoIds(query: String, maxResults: Int = 10) {
        viewModelScope.launch {
            _searchIdsState.value = Result.Loading

            val idsParams = SearchVideoIdsUseCase.Params(
                query = query,
                maxResults = maxResults
            )

            val result = searchVideoIdsUseCase(idsParams)
            _searchIdsState.value = when (result) {
                is UseCaseResult.Success -> {
                    Result.Success(result.data)
                }
                is UseCaseResult.Failure -> {
                    Result.Error(result.error)
                }
                else -> {
                    Result.Error(Exception("Unknown error"))
                }
            }
        }
    }

    fun resetVideoUrlState() {
        _videoUrlState.value = Result.Idle
    }

    fun test() {
        // Example usage - search for detailed video information
        searchVideos("kotlin tutorial", 10)

        // Example usage - search for video IDs only
        searchVideoIds("music", 20)
    }

    // Method to check if video is in playlist
    fun isInPlaylist(video: VideoSearchResult): Boolean {
        var isInPlaylist = false
        viewModelScope.launch {
            try {
                isInPlaylist = playlistRepository.isInPlaylist(video)
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to check playlist", e)
            }
        }
        return isInPlaylist
    }

    // Method to check if video is in history
    fun isInHistory(video: VideoSearchResult): Boolean {
        var isInHistory = false
        viewModelScope.launch {
            try {
                isInHistory = historyRepository.isInHistory(video)
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to check history", e)
            }
        }
        return isInHistory
    }

    fun setVideoUrl(url: String) {
        currentVideoUrl.value = url
    }

    fun setVideoAndStartSession(videoInfo: VideoSearchResult) {
        viewModelScope.launch {
            try {
                // Set current video info immediately
                _currentVideoInfo.value = videoInfo

                // Add to history
                addToHistory(videoInfo)

                val videoUrl = "https://www.youtube.com/watch?v=${videoInfo.id}"
                val result = getVideoUrlUseCase(GetVideoUrlUseCase.Params(videoUrl))

                when (result) {
                    is UseCaseResult.Success -> {
                        // Set the streaming URL which will trigger playback in PlayerOverlay
                        currentVideoUrl.value = result.data
                    }
                    is UseCaseResult.Failure -> {
                        // Handle error - you might want to show a toast or error message
                        android.util.Log.e("MainViewModel", "Failed to get streaming URL", result.error)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error setting video and starting session", e)
            }
        }
    }
}

sealed class Result<out T> {
    object Idle : Result<Nothing>()
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}