package com.t1h2h0.videos.views

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.t1h2h0.videos.utils.VideoSearchResult
import com.t1h2h0.videos.viewmodels.MainViewModel
import com.t1h2h0.videos.viewmodels.Result
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onVideoSelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResultsState.collectAsState()
    val predictions by viewModel.searchPredictionsState.collectAsState()
    val isLoading by remember { mutableStateOf(false) }
    var showPredictions by remember { mutableStateOf(false) }

    // Debounced search effect for predictions
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank() && searchQuery.length >= 2) {
            delay(300) // Debounce for 300ms
            viewModel.getSearchPredictions(searchQuery)
            showPredictions = true
        } else {
            showPredictions = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Input Section
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                if (it.isBlank()) {
                    showPredictions = false
                }
            },
            label = { Text("Search For Videos...") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    viewModel.searchVideos(searchQuery, 20)
                    showPredictions = false
                }
            ),
            trailingIcon = {
                IconButton(onClick = {
                    viewModel.searchVideos(searchQuery, 20)
                    showPredictions = false
                }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Predictions Section
        if (showPredictions && searchQuery.isNotBlank()) {
            when (predictions) {
                is Result.Loading -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                    }
                }
                is Result.Success -> {
                    val predictionList = (predictions as Result.Success).data
                    if (predictionList.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                predictionList.take(5).forEach { prediction ->
                                    Text(
                                        text = prediction,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                searchQuery = prediction
                                                viewModel.searchVideos(prediction, 20)
                                                showPredictions = false
                                            }
                                            .padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (prediction != predictionList.last()) {
                                        HorizontalDivider(thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    }
                }
                is Result.Error -> {
                    // Silently handle prediction errors, don't show to user
                }
                else -> { /* Handle other states */ }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Results Section
        when (searchResults) {
            is Result.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Result.Success -> {
                val videos = (searchResults as Result.Success).data
                if (videos.isEmpty()) {
                    Text("No results found.", modifier = Modifier.fillMaxWidth())
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(videos) { video ->
                            VideoSearchResultItem(video = video) {
                                viewModel.getVideoUrl(video.url)
                                viewModel.setCurrentVideo(video, video.url)
                            }
                        }
                    }
                }
            }
            is Result.Error -> {
                val error = (searchResults as Result.Error).exception.message
                Text("Error: ${error ?: "Unknown error"}", color = MaterialTheme.colorScheme.error)
            }
            is Result.Idle -> {
                Text("Start typing to see suggestions, or search for videos")
            }
            else -> { /* Initial state or other cases */ }
        }

        val videoUrlResult by viewModel.videoUrlState.collectAsState()
        LaunchedEffect(videoUrlResult) {
            if (videoUrlResult is Result.Success) {
                val streamingUrl = (videoUrlResult as Result.Success).data
                onVideoSelected(streamingUrl)
                viewModel.resetVideoUrlState()
            }
        }
    }
}

@Composable
fun VideoSearchResultItem(video: VideoSearchResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = "Video thumbnail",
                modifier = Modifier
                    .size(90.dp, 60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                onLoading = {
                    Log.d("CoilDebug", "Loading started for ${video.thumbnailUrl}")
                },
                onSuccess = {
                    Log.d("CoilDebug", "SUCCESS loading ${video.thumbnailUrl}")
                },
                onError = {
                    Log.e("CoilDebug", "FAILED loading ${video.thumbnailUrl}: ${it.result.throwable}")
                }
            )

            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(text = video.title, style = MaterialTheme.typography.titleMedium)
                Text(text = video.channelName ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}