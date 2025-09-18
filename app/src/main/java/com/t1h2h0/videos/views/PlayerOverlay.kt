package com.t1h2h0.videos.views

import android.content.Context
import android.content.pm.ActivityInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.PlayerView
import com.t1h2h0.videos.R
import com.t1h2h0.videos.viewmodels.MainViewModel
import com.t1h2h0.videos.services.BackgroundPlaybackService
import androidx.media3.session.SessionToken
import androidx.media3.session.MediaController
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.app.Activity
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.t1h2h0.videos.utils.ThumbnailCache
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

// Additional imports for search panel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import com.t1h2h0.videos.utils.VideoSearchResult

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerOverlay(
    viewModel: MainViewModel,
    onClosePlayer: () -> Unit,
    onShowHistory: () -> Unit = {},
    onShowPlaylist: () -> Unit = {},
    onSearchSelected: (url:String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val activity = context as? Activity

    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var isFullScreen by rememberSaveable { mutableStateOf(false) }

    // Search panel state
    var showSearchPanel by remember { mutableStateOf(false) }

    // State to track playback position and playing state
    val playbackPosition by viewModel.playbackPosition.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentVideoInfo by viewModel.currentVideoInfo.collectAsState()

    // Create ThumbnailCache instance that persists across recompositions
    val thumbnailCache = remember { ThumbnailCache() }

    // Player state tracking
    var shouldRestorePlayback by rememberSaveable { mutableStateOf(true) }
    var savedPosition by rememberSaveable { mutableStateOf(0L) }
    var wasPlaying by rememberSaveable { mutableStateOf(false) }

    // Function to play video with proper media item setup
    fun playVideo(url: String, videoInfo: VideoSearchResult? = null) {
        mediaController?.let { controller ->
            // Save current playback state before setting new media
            savedPosition = 0L // Reset position for new video
            wasPlaying = false
            shouldRestorePlayback = false

            // Get video info from viewModel or use provided videoInfo
            val currentVideoInfo = videoInfo ?: viewModel.getCurrentVideoInfo()

            // Load thumbnail with callback
            thumbnailCache.getThumbnailByteArray(currentVideoInfo?.thumbnailUrl) { artworkByteArray ->
                Handler(Looper.getMainLooper()).post {
                    try {
                        val mediaItem = androidx.media3.common.MediaItem.Builder()
                            .setUri(android.net.Uri.parse(url))
                            .setMediaMetadata(
                                androidx.media3.common.MediaMetadata.Builder()
                                    .setTitle(currentVideoInfo?.title ?: "Playing Video")
                                    .setArtist(currentVideoInfo?.uploaderName ?: "")
                                    .setAlbumTitle(currentVideoInfo?.title ?: "")
                                    .setArtworkUri(android.net.Uri.parse(currentVideoInfo?.thumbnailUrl ?: ""))
                                    .apply {
                                        artworkByteArray?.let {
                                            setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                                        }
                                    }
                                    .build()
                            )
                            .build()

                        controller.setMediaItem(mediaItem)
                        controller.prepare()
                        controller.play() // Auto-play the new video

                        Log.d("PlayerOverlay", "Playing video: ${currentVideoInfo?.title}")
                    } catch (e: Exception) {
                        Log.e("PlayerOverlay", "Error setting media item with artwork", e)
                    }
                }
            }
        }
    }

    // Player listener for tracking playback state
    val playerListener = remember {
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        // Restore playback position if needed
                        if (shouldRestorePlayback && savedPosition > 0) {
                            mediaController?.seekTo(savedPosition)
                            if (wasPlaying) {
                                mediaController?.play()
                            }
                            shouldRestorePlayback = false
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val position = mediaController?.currentPosition ?: 0L
                viewModel.updatePlaybackState(position, isPlaying)
            }
        }
    }

    // Periodic position updates
    LaunchedEffect(mediaController) {
        while (mediaController != null) {
            mediaController?.let { controller ->
                val position = controller.currentPosition
                val playing = controller.isPlaying
                viewModel.updatePlaybackState(position, playing)
            }
            delay(1000) // Update every second
        }
    }

    // --- MediaController connection logic ---
    LaunchedEffect(Unit) {
        val sessionToken = SessionToken(context, android.content.ComponentName(context, BackgroundPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            try {
                val controller = controllerFuture.get()
                mediaController = controller
                viewModel.mediaController.value = controller

                // Add listener for playback state changes
                controller.addListener(playerListener)

                // If a video URL is set, start playback immediately
                viewModel.currentVideoUrl.value?.let { url ->
                    playVideo(url)
                    viewModel.currentVideoUrl.value = null
                }
            } catch (e: Exception) {
                Log.e("PlayerOverlay", "Error connecting to MediaController", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // --- Lifecycle management for MediaController ---
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // Save playback state before configuration change
                    mediaController?.let { controller ->
                        savedPosition = controller.currentPosition
                        wasPlaying = controller.isPlaying
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Restore playback state after configuration change
                    if (savedPosition > 0) {
                        shouldRestorePlayback = true
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    mediaController?.removeListener(playerListener)
                    mediaController?.release()
                    mediaController = null
                    viewModel.mediaController.value = null
                    thumbnailCache.clearCache()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // --- Fullscreen and System UI management ---
    SideEffect {
        if (activity != null) {
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, view)

            if (isFullScreen) {
                // Hide system bars
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                // Show system bars
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // --- System Back Button Handler ---
    BackHandler(enabled = true) {
        when {
            showSearchPanel -> showSearchPanel = false
            isFullScreen -> isFullScreen = false
            else -> onClosePlayer()
        }
    }

    // --- Player UI ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
    ) {
        // Main player view
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isFullScreen) Modifier.fillMaxSize() else Modifier.height(250.dp))
                .align(Alignment.Center),
            factory = { context ->
                val playerView = LayoutInflater.from(context).inflate(R.layout.player_view_layout, null) as PlayerView
                playerView.useController = true
                playerView.controllerShowTimeoutMs = 3000
                playerView.keepScreenOn = true
                playerView
            },
            update = { playerView ->
                playerView.player = mediaController
            }
        )

        // Top controls row
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.clickable { onClosePlayer() }
            )
            Spacer(modifier = Modifier.width(16.dp))

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White,
                modifier = Modifier.clickable {
                    showSearchPanel = !showSearchPanel
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "History",
                tint = Color.White,
                modifier = Modifier.clickable { onShowHistory() }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Icon(
                imageVector = Icons.Default.PlaylistAdd,
                contentDescription = "Add to Playlist",
                tint = Color.White,
                modifier = Modifier.clickable {
                    currentVideoInfo?.let { video ->
                        viewModel.addToPlaylist(video)
                        Toast.makeText(context, "Added ${video.title} to playlist", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Icon(
                imageVector = Icons.Default.PlaylistPlay,
                contentDescription = "Playlist",
                tint = Color.White,
                modifier = Modifier.clickable {
                    onShowPlaylist()
                }
            )
        }

        // Top right controls
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            // Fullscreen toggle button
            Icon(
                imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (isFullScreen) "Exit Fullscreen" else "Enter Fullscreen",
                tint = Color.White,
                modifier = Modifier.clickable {
                    isFullScreen = !isFullScreen
                }
            )
        }

        // Search panel overlay
        AnimatedVisibility(
            visible = showSearchPanel,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .zIndex(10f)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(if (isFullScreen) 400.dp else 350.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                color = Color.Black.copy(alpha = 0.95f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Search panel header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Text(
                            text = "Search Videos",
                            color = Color.White,
                            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Search",
                            tint = Color.White,
                            modifier = Modifier.clickable { showSearchPanel = false }
                        )
                    }

                    // Embedded SearchScreen
                    SearchScreen(
                        viewModel = viewModel,
                        onVideoSelected = { streamingUrl ->
                            // Play the video directly here instead of using the callback
                            playVideo(streamingUrl)
                            showSearchPanel = false // Close search panel after selection
                        }
                    )
                }
            }
        }
    }
}