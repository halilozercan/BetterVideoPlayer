package com.example.composevideoplayer

import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import androidx.annotation.RawRes
import androidx.compose.*
import androidx.ui.core.Alignment
import androidx.ui.core.ContextAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.drawShadow
import androidx.ui.foundation.*
import androidx.ui.graphics.Color
import androidx.ui.layout.*
import androidx.ui.material.Button
import androidx.ui.material.IconButton
import androidx.ui.material.LinearProgressIndicator
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Pause
import androidx.ui.material.icons.filled.PlayArrow
import androidx.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun VideoPlayer(@RawRes source: Int) {

    val context = ContextAmbient.current

    val sourceUri = Uri.parse("android.resource://${context.packageName}/$source")

    lateinit var playerSurfaceController: PlayerSurfaceController
    lateinit var playerSurfaceCallback: PlayerSurfaceCallback
    lateinit var mediaPlayer: MediaPlayer

    var playButtonUiState by mutableStateOf(false)
    val progressIndicatorUiState = mutableStateOf(0f)

    var controlsVisible by mutableStateOf(false)

    mediaPlayer = remember { MediaPlayer().apply {
        setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build()
        )

        setOnPreparedListener {
            it.start()
            playButtonUiState = it.isPlaying
        }

        setOnVideoSizeChangedListener { _, width, height ->
            playerSurfaceController.videoSizeChanged(width, height)
        }
    } }

    launchInComposition {
        repeat(1000000) {
            activeListener(progressIndicatorUiState, mediaPlayer)
        }
    }

    playerSurfaceCallback = remember { object: PlayerSurfaceCallback {
        override fun surfaceAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            mediaPlayer.setSurface(Surface(surfaceTexture))
            mediaPlayer.setDataSource(context, sourceUri)
            mediaPlayer.prepareAsync()
        }
    } }

    Providers(ContentColorAmbient provides Color.White) {
        Stack(modifier = Modifier.heightIn(maxHeight = 200.dp)) {
            Box(modifier = Modifier.fillMaxSize().clickable(indication = null, onClick = {
                controlsVisible = !controlsVisible
            })) {
                playerSurfaceController = PlayerSurface(playerSurfaceCallback)
            }

            if(controlsVisible) {
                LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().gravity(Alignment.BottomCenter),
                        progress = progressIndicatorUiState.value
                )

                IconButton(
                        onClick = {
                            mediaPlayer.toggle()
                            playButtonUiState = mediaPlayer.isPlaying
                        },
                        modifier = Modifier.gravity(Alignment.Center)
                ) {
                    Icon(
                        asset = if (playButtonUiState) Icons.Filled.Pause else Icons.Filled.PlayArrow
                    )
                }


            }
        }
    }

}

fun MediaPlayer.toggle() {
    if(this.isPlaying) pause() else start()
}

suspend fun activeListener(
        progressIndicatorUiState: MutableState<Float>,
        mediaPlayer: MediaPlayer
) {
    var pos = mediaPlayer.currentPosition.toLong()
    val dur = mediaPlayer.duration.toLong()
    if (pos > dur) pos = dur

    progressIndicatorUiState.value = (pos.toFloat()/dur.toFloat())

    delay(100)
}