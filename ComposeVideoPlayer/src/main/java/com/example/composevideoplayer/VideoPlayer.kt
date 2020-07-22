package com.example.composevideoplayer

import android.util.Log
import androidx.annotation.RawRes
import androidx.compose.*
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.onPositioned
import androidx.ui.foundation.ContentColorAmbient
import androidx.ui.graphics.Color
import androidx.ui.layout.*

val VideoPlayerControllerAmbient = ambientOf<VideoPlayerController> { error("VideoPlayerController is not initialized") }

sealed class VideoPlayerSource {
    data class Raw(@RawRes val resId: Int) : VideoPlayerSource()
    data class Network(val url: String, val headers: Map<String, String> = mapOf()) : VideoPlayerSource()
}

@Composable
fun VideoPlayer(
        controller: VideoPlayerController,
        modifier: Modifier = Modifier
) {
    val controlsEnabled by controller.controlsEnabled.collectAsState()
    val controlsVisible by controller.controlsVisible.collectAsState()

    val videoSize by controller.videoSize.collectAsState()

    Providers(
            ContentColorAmbient provides Color.White,
            VideoPlayerControllerAmbient provides controller
    ) {
        Stack(modifier = Modifier.fillMaxWidth()
                .aspectRatio(videoSize.width / videoSize.height)
                .onPositioned {
                    Log.d("DragDistance", "${it.size}")
                } + modifier) {
            PlayerSurface()

            if (controlsEnabled && !controlsVisible) {
                MediaControlGestures(
                        onClick = { controller.showControls() },
                        modifier = Modifier.matchParentSize()
                )
            }

            if (controlsEnabled && controlsVisible) {
                MediaControlButtons(modifier = Modifier.matchParentSize())
            }
        }
    }

    onDispose {
        controller.onDispose()
    }
}