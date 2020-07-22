package com.example.composevideoplayer

import android.content.Context
import android.net.Uri
import androidx.compose.*
import androidx.ui.geometry.Size
import androidx.ui.graphics.Color
import com.example.composevideoplayer.util.set
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


class VideoPlayerController(
    private val context: Context,
    initialSource: VideoPlayerSource,
    override val coroutineContext: CoroutineContext = EmptyCoroutineContext
) : MediaPlaybackControls, CoroutineScope, VideoPlayerState {

    private var source: VideoPlayerSource = initialSource

    var mediaPositionTrackerJob: Job? = null

    override fun play() {
        if(exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0)
        }
        exoPlayer.playWhenReady = true
    }

    override fun pause() {
        exoPlayer.playWhenReady = false
    }

    override fun playPauseToggle() {
        if(exoPlayer.isPlaying) pause()
        else play()
    }

    override fun quickSeekForward() {
        val target = (exoPlayer.currentPosition + 10_000).coerceAtMost(exoPlayer.duration)
        exoPlayer.seekTo(target)
    }

    override fun quickSeekBackward() {
        val target = (exoPlayer.currentPosition - 10_000).coerceAtLeast(0)
        exoPlayer.seekTo(target)
    }

    override fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    override fun setSource(source: VideoPlayerSource) {
        this.source = source
        prepare()
    }

    private var initializedWithSource = true

    private val exoPlayer = SimpleExoPlayer.Builder(context)
            .build()
            .apply {
                playWhenReady = true
                addListener(object : Player.EventListener {

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        this@VideoPlayerController.isPlaying.value = isPlaying
                    }

                    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                        super.onPlayerStateChanged(playWhenReady, playbackState)
                        this@VideoPlayerController.playbackState.value = PlaybackState.of(playbackState)
                    }
                })

                addVideoListener(object: VideoListener {
                    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                        super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)

                        this@VideoPlayerController.videoSize.value =
                                Size(width.toFloat(), height.toFloat())
                    }
                })
            }


    fun enableControls() { controlsEnabled.value = true }

    fun disableControls() { controlsEnabled.value = false }

    fun showControls() {
        controlsVisible.value = true
    }

    fun hideControls() { controlsVisible.value = false }

    fun toggleControls() { controlsVisible.value = !controlsVisible.value }

    private fun prepare() {
        mediaPositionTrackerJob?.cancel()

        mediaPositionTrackerJob = launch {
            repeat(1000000) {

                duration.value = exoPlayer.duration
                currentPosition.value = exoPlayer.currentPosition

                delay(250)
            }
        }

        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(context,
                Util.getUserAgent(context, context.packageName))

        val videoSource: MediaSource = when (val source = source) {
            is VideoPlayerSource.Raw -> {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(RawResourceDataSource.buildRawResourceUri(source.resId))
            }
            is VideoPlayerSource.Network -> {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(Uri.parse(source.url))
            }
        }

        exoPlayer.prepare(videoSource)
    }

    internal fun onDispose() {
        mediaPositionTrackerJob?.cancel()
        exoPlayer.release()
    }

    internal fun playerViewAvailable(playerView: PlayerView) {
        playerView.player = exoPlayer
        playerView.setBackgroundColor(context.getColor(android.R.color.black))
        if (initializedWithSource) {
            initializedWithSource = false
            prepare()
        }
    }

    override val isPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val controlsVisible: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val controlsEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val duration: MutableStateFlow<Long> = MutableStateFlow(1L)
    override val currentPosition: MutableStateFlow<Long> = MutableStateFlow(1L)

    override val videoSize: MutableStateFlow<Size> = MutableStateFlow(Size(1280f,720f))
    override val draggingProgressText: MutableStateFlow<String?> = MutableStateFlow(null)
    override val playbackState: MutableStateFlow<PlaybackState> = MutableStateFlow(PlaybackState.IDLE)
}