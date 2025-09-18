package com.t1h2h0.videos.views

import android.content.ComponentName
import android.content.pm.ActivityInfo // For fullscreen orientation
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager

import dagger.hilt.android.AndroidEntryPoint
import com.t1h2h0.videos.databinding.FragmentMainBinding
import com.t1h2h0.videos.viewmodels.MainViewModel
import com.t1h2h0.videos.viewmodels.Result
import com.t1h2h0.videos.utils.VideoSearchResult
import com.t1h2h0.videos.services.BackgroundPlaybackService

private const val TAG = "MainFragment"
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding
    private val viewModel: MainViewModel by viewModels()
    private var mediaController: MediaController? = null
    private lateinit var searchResultsAdapter: SearchResultsAdapter
    // Removed: private var isSearchMode = false (it's implicitly true now)

    private var currentVideoTitle = ""
    private var currentChannelName = ""
    private var currentThumbnailUrl = ""
    private var currentVideoUrl = ""

    private var isFullScreen = false // For fullscreen handling

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        initializeMediaController()

        // Handle system back button
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.playerView.visibility == View.VISIBLE) { // If player UI is currently visible
                    if (isFullScreen) {
                        exitFullScreen()
                        Log.d(TAG, "System back pressed: Exiting fullscreen.")
                    } else {
                        showSearchResults() // Back to search results from player in normal mode
                        Log.d(TAG, "System back pressed: Hiding player UI, showing search results.")
                    }
                } else {
                    // If player UI is not visible (already in search results), let default back behavior happen
                    Log.d(TAG, "System back pressed: Default behavior (e.g., pop fragment/exit app).")
                    isEnabled = false // Disable this callback temporarily
                    requireActivity().onBackPressedDispatcher.onBackPressed() // Trigger the default system back behavior
                }
            }
        })
    }

    private fun setupUI() {
        // Setup RecyclerView for search results
        searchResultsAdapter = SearchResultsAdapter { videoResult ->
            onVideoSelected(videoResult)
        }

        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchResultsAdapter
        }

        // Removed: Toggle search button listener
        // Removed: URL input button listener

        // Search button
        binding.searchButton.setOnClickListener {
            performSearch()
        }

        // Handle Enter key press in search EditText
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        // Back button to return to search results
        binding.backButton.setOnClickListener {
            showSearchResults()
        }

        // Fullscreen toggle button (assuming you've added this to your XML)
        binding.fullscreenToggle.setOnClickListener {
            if (isFullScreen) {
                exitFullScreen()
            } else {
                enterFullScreen()
            }
        }

        // Initial UI state: always show search container and results
        binding.searchContainer.visibility = View.VISIBLE
        binding.searchResultsRecyclerView.visibility = View.VISIBLE
        binding.playerView.visibility = View.GONE // Player is initially hidden
        binding.backButton.visibility = View.GONE
        binding.searchProgressBar.visibility = View.GONE
        // Removed: binding.urlContainer.visibility = View.GONE
        // Removed: binding.toggleSearchButton.visibility = View.GONE
    }

    private fun performSearch() {
        val query = binding.searchEditText.text.toString().trim()
        if (query.isNotEmpty()) {
            Log.d(TAG, "Performing search for: $query")
            viewModel.searchVideos(query, 20)
        } else {
            Log.d(TAG, "Search query is empty")
        }
    }

    private fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.videoUrlState.collect { result ->
                when (result) {
                    is Result.Success -> {
                        Log.d(TAG, "url: ${result.data}")
                        currentVideoUrl = result.data
                        playVideoInService(result.data)
                        showPlayer()
                    }
                    is Result.Error -> {
                        result.exception.message?.let { Log.e(TAG, "Error: $it") }
                        // TODO: Show error message to user
                    }
                    is Result.Loading -> {
                        Log.d(TAG, "Loading...")
                        // TODO: Show loading indicator
                    }

                    Result.Idle -> {

                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.searchResultsState.collect { result ->
                when (result) {
                    is Result.Success -> {
                        Log.d(TAG, "Search results: ${result.data.size} videos found")
                        searchResultsAdapter.submitList(result.data)
                        binding.searchProgressBar.visibility = View.GONE

                        if (result.data.isNotEmpty()) {
                            binding.searchResultsRecyclerView.visibility = View.VISIBLE
                        }
                    }
                    is Result.Error -> {
                        result.exception.message?.let { Log.e(TAG, "Search Error: $it") }
                        binding.searchProgressBar.visibility = View.GONE
                        // TODO: Show error message to user
                    }
                    is Result.Loading -> {
                        Log.d(TAG, "Searching...")
                        binding.searchProgressBar.visibility = View.VISIBLE
                    }

                    Result.Idle -> {

                    }
                }
            }
        }
    }

    // Removed toggleSearchMode() as it's no longer needed

    private fun onVideoSelected(videoResult: VideoSearchResult) {
        Log.d(TAG, "Selected video: ${videoResult.title}")
        currentVideoTitle = videoResult.title
        currentChannelName = videoResult.channelName ?: ""
        currentThumbnailUrl = videoResult.thumbnailUrl ?: ""
        currentVideoUrl = videoResult.url

        viewModel.getVideoUrl(videoResult.url)
    }

    private fun showPlayer() {
        binding.playerView.visibility = View.VISIBLE
        binding.backButton.visibility = View.VISIBLE
        binding.fullscreenToggle.visibility = View.VISIBLE // Show fullscreen button
        binding.searchResultsRecyclerView.visibility = View.GONE
        // Removed: binding.urlContainer.visibility = View.GONE
        binding.searchContainer.visibility = View.GONE
        // Removed: binding.toggleSearchButton.visibility = View.GONE
        binding.searchProgressBar.visibility = View.GONE

    }

    private fun showSearchResults() {
        binding.playerView.visibility = View.GONE
        binding.backButton.visibility = View.GONE
        binding.fullscreenToggle.visibility = View.GONE // Hide fullscreen button
        // Removed: binding.toggleSearchButton.visibility = View.VISIBLE (no longer exists)

        binding.searchContainer.visibility = View.VISIBLE
        binding.searchResultsRecyclerView.visibility = View.VISIBLE
        // Removed: binding.urlContainer.visibility = View.GONE (no longer exists)

        // Ensure we are out of fullscreen when returning to search
        if (isFullScreen) {
            exitFullScreen()
        }
    }

    private fun hidePlayer() {
        binding.playerView.visibility = View.GONE
        binding.backButton.visibility = View.GONE
        binding.fullscreenToggle.visibility = View.GONE // Hide fullscreen button
        binding.searchProgressBar.visibility = View.GONE
        // Removed: binding.urlContainer.visibility = View.GONE
        // Removed: binding.searchContainer.visibility = View.GONE
        // Removed: binding.toggleSearchButton.visibility = View.GONE

        // Ensure we are out of fullscreen if player is hidden by other means
        if (isFullScreen) {
            exitFullScreen()
        }
    }

    private fun initializeMediaController() {
        if (mediaController != null) {
            Log.d(TAG, "MediaController already initialized. Reusing.")
            return
        }

        Log.d(TAG, "Initializing MediaController...")
        val sessionToken = SessionToken(requireContext(), ComponentName(requireContext(), BackgroundPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(requireContext(), sessionToken).buildAsync()

        controllerFuture.addListener({
            try {
                mediaController = controllerFuture.get()
                binding.playerView.player = mediaController // Attach PlayerView to the controller
                binding.playerView.keepScreenOn = true // Keep screen on during playback
                Log.d(TAG, "MediaController connected successfully. PlayerView.keepScreenOn set to true.")

                mediaController?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        Log.d(TAG, "MediaController's Player.Listener - Is playing changed: $isPlaying")
                    }
                })

            } catch (e: Exception) {
                Log.e(TAG, "Error connecting MediaController: ${e.message}", e)
                mediaController = null
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun playVideoInService(videoUrl: String) {
        mediaController?.let { controller ->
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(videoUrl))
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(currentVideoTitle)
                        .setArtist(currentChannelName)
                        .build()
                )
                .build()

            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
            Log.d(TAG, "Sent play command to MediaController for URL: $videoUrl")
        } ?: run {
            Log.w(TAG, "MediaController not yet available. Cannot play video in service.")
        }
    }

    // --- Fullscreen helper methods ---
    private fun enterFullScreen() {
        if (activity == null) return

        isFullScreen = true
        Log.d(TAG, "Entering fullscreen.")

        // Hide system UI (status bar, navigation bar)
        @Suppress("DEPRECATION") // Use WindowInsetsController for API 30+ for modern approach
        activity?.window?.decorView?.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )

        // Make PlayerView fill the screen
        val params = binding.playerView.layoutParams
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = ViewGroup.LayoutParams.MATCH_PARENT
        binding.playerView.layoutParams = params

        // Optionally, change orientation to landscape
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Hide other UI elements when in fullscreen
//        binding.urlContainer.visibility = View.GONE // This might give an error if urlContainer is fully removed from XML
        binding.searchContainer.visibility = View.GONE
        binding.searchResultsRecyclerView.visibility = View.GONE
//        binding.toggleSearchButton.visibility = View.GONE // This might give an error if toggleSearchButton is fully removed from XML
        binding.backButton.visibility = View.GONE
        binding.fullscreenToggle.visibility = View.GONE // Hide the button itself when in fullscreen
    }

    private fun exitFullScreen() {
        if (activity == null) return

        isFullScreen = false
        Log.d(TAG, "Exiting fullscreen.")

        // Show system UI
        @Suppress("DEPRECATION")
        activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE

        // Restore PlayerView size
        val params = binding.playerView.layoutParams
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = resources.getDimensionPixelSize(com.t1h2h0.videos.R.dimen.player_height) // Restore original height
        binding.playerView.layoutParams = params

        // Restore orientation to portrait (or unspecified)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        // Show other relevant UI elements again (now that it's only search mode)
        binding.searchContainer.visibility = View.VISIBLE
        binding.searchResultsRecyclerView.visibility = View.VISIBLE
        // Removed: binding.urlContainer.visibility = View.GONE
        // Removed: binding.toggleSearchButton.visibility = View.VISIBLE

        binding.backButton.visibility = View.VISIBLE // Show back button again
        binding.fullscreenToggle.visibility = View.VISIBLE // Show fullscreen button again
    }


    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Fragment paused. Playback continues in service via MediaController.")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Fragment resumed. MediaController connected: ${mediaController != null}")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "Fragment stopped. Playback continues in service via MediaController.")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaController?.release()
        mediaController = null
        Log.d(TAG, "MediaController released.")
        // Ensure we exit fullscreen if the view is destroyed while in fullscreen (unlikely but safe)
        if (isFullScreen) {
            exitFullScreen()
        }
    }
}