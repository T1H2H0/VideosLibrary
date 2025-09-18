package com.t1h2h0.videos.views

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.t1h2h0.videos.viewmodels.MainViewModel

@Composable
fun MainAppContent() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = "player_screen") {

        composable("search_screen") {
            SearchScreen(
                viewModel = mainViewModel,
                onVideoSelected = { videoUrl ->
                    mainViewModel.currentVideoUrl.value = videoUrl
                    navController.navigate("player_screen")
                }
            )
        }

        composable("history") {
            HistoryScreen(
                viewModel = mainViewModel,
                onVideoSelected = { streamingUrl ->
                    mainViewModel.currentVideoUrl.value = streamingUrl
                    navController.navigate("player_screen")
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }

        composable("playlist") {
            PlaylistScreen(
                viewModel = mainViewModel,
                onVideoSelected = { streamingUrl ->
                    mainViewModel.currentVideoUrl.value = streamingUrl
                    navController.navigate("player_screen")
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }

        composable("player_screen") {

            PlayerOverlay(
                viewModel = mainViewModel,
                onSearchSelected = { streamingUrl ->
                    mainViewModel.currentVideoUrl.value = streamingUrl

                },
                onClosePlayer = {
                    mainViewModel.mediaController.value?.stop()
                    navController.popBackStack()
                    mainViewModel.currentVideoUrl.value = null
                },
                onShowHistory = {
                    navController.navigate("history")
                },
                onShowPlaylist = {
                    navController.navigate("playlist")
                }
            )
        }

    }

    }