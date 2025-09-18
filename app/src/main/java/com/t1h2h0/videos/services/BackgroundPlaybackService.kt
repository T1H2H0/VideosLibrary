package com.t1h2h0.videos.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaSessionService
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Size
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.t1h2h0.videos.R
import com.t1h2h0.videos.MainActivity
import java.net.URL
import kotlinx.coroutines.*
import androidx.core.net.toUri

@UnstableApi
class BackgroundPlaybackService : MediaSessionService() {
    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.d(TAG, "Service ExoPlayer Playback state changed: $playbackState")
            updateNotificationSafely()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(TAG, "Service ExoPlayer Is playing changed: $isPlaying")
            isPlayingState = isPlaying
            updateNotificationSafely()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            Log.d(TAG, "Media metadata changed: ${mediaMetadata.title}")
            // Update internal variables from the actual media metadata
            videoTitle = mediaMetadata.title?.toString() ?: "Playing Video"
            channelName = mediaMetadata.artist?.toString() ?: ""
            updateNotificationSafely()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        // Initialize ExoPlayer
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(), true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer?.addListener(playerListener)

        // Create MediaSession with the REAL ExoPlayer (not custom player)
        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setId(MEDIA_SESSION_ID)
            .setCallback(mediaSessionCallback)


            .build()

        createNotificationChannel()
        registerMediaReceiver()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        Log.d(TAG, "ExoPlayer and MediaSession created in service")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        // Return the MediaSession that uses the real ExoPlayer
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)

        // Check if an explicit action was sent (e.g., from notification buttons)
        val action = intent?.action
        if (action != null && (action == ACTION_PLAY || action == ACTION_PAUSE || action == ACTION_STOP || action == ACTION_PREVIOUS || action == ACTION_NEXT)) {
            mediaReceiver.onReceive(this, intent)
            // If it's just an action, we might not need to re-handle the initial setup,
            // but for a foreground service, it's good to ensure it's still foregrounded.
            startForeground(NOTIFICATION_ID, createNotification()) // Re-assert foreground state
            return START_NOT_STICKY // Return START_NOT_STICKY for action intents
        }

        val videoTitle = intent?.getStringExtra("video_title") ?: "Playing Video"
        val channelName = intent?.getStringExtra("channel_name") ?: ""
        val thumbnailUrl = intent?.getStringExtra("thumbnail_url") ?: ""
        val autoPlay = intent?.getBooleanExtra("autoplay", true) ?: true
        val videoUrl = intent?.getStringExtra("video_url")

        if (!videoUrl.isNullOrEmpty()) {
            // Create MediaItem with proper metadata for the lock screen
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(videoTitle)
                .setArtist(channelName)
                .setDisplayTitle(videoTitle)
                .setSubtitle(channelName)
                .setArtworkUri(thumbnailUrl.toUri())
//                .setArtworkData()
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(videoUrl.toUri())
                .setMediaMetadata(mediaMetadata)
                .build()

            // Set media on the REAL ExoPlayer
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()

            if (autoPlay) {
                exoPlayer?.play()
            }

            // Update internal tracking
            updateTrackInfo(videoTitle, channelName, thumbnailUrl)
        }

        startForeground(NOTIFICATION_ID, createNotification())
        return result
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")

        unregisterMediaReceiver()
        serviceScope.cancel() // Cancel all coroutines started in this scope

        // Release MediaSession first, then ExoPlayer
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null

        abandonAudioFocus()
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d(TAG, "ExoPlayer and MediaSession released in service.")
    }

    companion object {
        private const val TAG = "BackgroundPlaybackService"
        private const val CHANNEL_ID = "background_playback_channel"
        private const val NOTIFICATION_ID = 1
        private const val MEDIA_SESSION_ID = "BackgroundPlaybackService"

        // Action constants
        const val ACTION_PLAY = "action_play"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_STOP = "action_stop"
        const val ACTION_PREVIOUS = "action_previous"
        const val ACTION_NEXT = "action_next"

        // Custom session commands
        private const val CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_ON = "SHUFFLE_ON"
        private const val CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_OFF = "SHUFFLE_OFF"

        /**
         * Starts the background playback service.
         */
        fun startService(context: Context, videoTitle: String, channelName: String = "", thumbnailUrl: String = "", autoPlay: Boolean = true, videoUrl: String = "") {
            val intent = Intent(context, BackgroundPlaybackService::class.java).apply {
                putExtra("video_title", videoTitle)
                putExtra("channel_name", channelName)
                putExtra("thumbnail_url", thumbnailUrl)
                putExtra("autoplay", autoPlay)
                putExtra("video_url", videoUrl) // Add video URL parameter
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "Service start requested")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service", e)
            }
        }

        /**
         * Stops the background playback service.
         */
        fun stopService(context: Context) {
            val intent = Intent(context, BackgroundPlaybackService::class.java)
            context.stopService(intent)
        }

        /**
         * Sends a play/pause/stop action to the service.
         */
        fun sendPlayPauseAction(context: Context, action: String) {
            val intent = Intent(context, BackgroundPlaybackService::class.java).apply {
                this.action = action
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "Action sent: $action")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send action: $action", e)
            }
        }
    }

    private var videoTitle: String = "Playing Video"
    private var channelName: String = ""
    private var thumbnailUrl: String = ""
    private var isPlayingState: Boolean = false // Tracks actual playback state
    private var thumbnailBitmap: Bitmap? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var isReceiverRegistered = false

    // Update broadcast receiver to use ExoPlayer directly
    private val mediaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "MediaReceiver received action: ${intent?.action}")
            when (intent?.action) {
                ACTION_PLAY -> {
                    Log.d(TAG, "Play action received from broadcast")
                    if (requestAudioFocus()) {
                        exoPlayer?.play() // Use real ExoPlayer
                    }
                }
                ACTION_PAUSE -> {
                    Log.d(TAG, "Pause action received from broadcast")
                    exoPlayer?.pause() // Use real ExoPlayer
                }
                ACTION_STOP -> {
                    Log.d(TAG, "Stop action received from broadcast")
                    exoPlayer?.stop() // Use real ExoPlayer
                    abandonAudioFocus()
                    broadcastToFragment(ACTION_STOP)
                    stopSelf()
                }
                ACTION_PREVIOUS -> {
                    Log.d(TAG, "Previous action received from broadcast")
                    exoPlayer?.seekToPrevious() // Use real ExoPlayer
                    broadcastToFragment(ACTION_PREVIOUS)
                }
                ACTION_NEXT -> {
                    Log.d(TAG, "Next action received from broadcast")
                    exoPlayer?.seekToNext() // Use real ExoPlayer
                    broadcastToFragment(ACTION_NEXT)
                }
            }
        }
    }

    // Simplified MediaSession callback since ExoPlayer handles most of the work
    private val mediaSessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val availableSessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(availableSessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: android.os.Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_ON -> {
                    Log.d(TAG, "Custom command: SHUFFLE_ON")
                    exoPlayer?.shuffleModeEnabled = true
                }
                CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_OFF -> {
                    Log.d(TAG, "Custom command: SHUFFLE_OFF")
                    exoPlayer?.shuffleModeEnabled = false
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerMediaReceiver() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter().apply {
                addAction(ACTION_PLAY)
                addAction(ACTION_PAUSE)
                addAction(ACTION_STOP)
                addAction(ACTION_PREVIOUS)
                addAction(ACTION_NEXT)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(mediaReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    registerReceiver(mediaReceiver, intentFilter)
                }
                isReceiverRegistered = true
                Log.d(TAG, "MediaReceiver registered")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register receiver", e)
            }
        }
    }

    private fun unregisterMediaReceiver() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(mediaReceiver)
                isReceiverRegistered = false
                Log.d(TAG, "MediaReceiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Receiver was already unregistered or not registered.", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification for background video playback"
                setSound(null, null)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    private fun requestAudioFocus(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (audioFocusRequest == null) {
                    val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(
                            android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setOnAudioFocusChangeListener { focusChange ->
                            handleAudioFocusChange(focusChange)
                        }
                        .build()
                    audioFocusRequest = focusRequest
                }
                val result = audioManager.requestAudioFocus(audioFocusRequest!!)
                hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                Log.d(TAG, "Audio focus request result: $hasAudioFocus")
                hasAudioFocus
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.requestAudioFocus(
                    { focusChange -> handleAudioFocusChange(focusChange) },
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
                hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                Log.d(TAG, "Audio focus request result (legacy): $hasAudioFocus")
                hasAudioFocus
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting audio focus", e)
            false
        }
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Audio focus lost")
                exoPlayer?.pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus gained")
                // User can manually resume if needed
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus lost transiently (can duck)")
                // Optionally lower volume
            }
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
            hasAudioFocus = false
            Log.d(TAG, "Audio focus abandoned")
        } catch (e: Exception) {
            Log.e(TAG, "Error abandoning audio focus", e)
        }
    }

    private fun loadThumbnail() {
        if (thumbnailUrl.isNotEmpty()) {
            serviceScope.launch {
                try {
                    thumbnailBitmap = withContext(Dispatchers.IO) {
                        val url = URL(thumbnailUrl)
                        BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    }
                    updateNotificationSafely()
                    Log.d(TAG, "Thumbnail loaded successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load thumbnail", e)
                    thumbnailBitmap = null
                    updateNotificationSafely()
                }
            }
        } else {
            thumbnailBitmap = null
            updateNotificationSafely()
        }
    }

    // Update notification to get state from real ExoPlayer
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get current playing state from real ExoPlayer
        val isCurrentlyPlaying = exoPlayer?.isPlaying ?: false

        val playPauseAction = if (isCurrentlyPlaying) {
            NotificationCompat.Action(
                R.drawable.ic_pause, "Pause",
                createPendingIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_play_arrow, "Play",
                createPendingIntent(ACTION_PLAY)
            )
        }

        val previousAction = NotificationCompat.Action(
            R.drawable.ic_skip_previous, "Previous",
            createPendingIntent(ACTION_PREVIOUS)
        )

        val nextAction = NotificationCompat.Action(
            R.drawable.ic_skip_next, "Next",
            createPendingIntent(ACTION_NEXT)
        )

        val stopAction = NotificationCompat.Action(
            R.drawable.ic_stop, "Stop",
            createPendingIntent(ACTION_STOP)
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(videoTitle)
            .setContentText(if (channelName.isNotEmpty()) channelName else "Playing in background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(isCurrentlyPlaying)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setShowWhen(false)

        thumbnailBitmap?.let { bitmap ->
            builder.setLargeIcon(bitmap)
        } ?: run {
            builder.setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round))
        }

        // Add actions to the notification
        builder.addAction(previousAction)
        builder.addAction(playPauseAction)
        builder.addAction(nextAction)
        builder.addAction(stopAction)

        // Use MediaSession for proper lock screen integration
        mediaSession?.let { session ->
            builder.setStyle(
                androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(0, 1, 2)

            )
        }

        return builder.build()
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, BackgroundPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updateNotificationSafely() {
        try {
            val notification = createNotification()
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing notification permission. Cannot update notification.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification", e)
        }
    }

    private fun broadcastToFragment(action: String) {
        val broadcastIntent = Intent("MEDIA_CONTROL_ACTION").apply {
            putExtra("action", action)
        }
        sendBroadcast(broadcastIntent)
        Log.d(TAG, "Broadcast sent: $action")
    }

    fun setPlayingState(playing: Boolean) {
        if (isPlayingState != playing) {
            isPlayingState = playing
            updateNotificationSafely()
        }
    }

    fun updateTrackInfo(title: String, channel: String = "", newThumbnailUrl: String = "") {
        var needsNotificationUpdate = false

        if (videoTitle != title) {
            videoTitle = title
            needsNotificationUpdate = true
        }
        if (channelName != channel) {
            channelName = channel
            needsNotificationUpdate = true
        }

        if (thumbnailUrl != newThumbnailUrl) {
            thumbnailUrl = newThumbnailUrl
            if (thumbnailUrl.isNotEmpty()) {
                loadThumbnail()
            } else {
                thumbnailBitmap = null
                needsNotificationUpdate = true
            }
        } else if (needsNotificationUpdate) {
            updateNotificationSafely()
        }
    }
}