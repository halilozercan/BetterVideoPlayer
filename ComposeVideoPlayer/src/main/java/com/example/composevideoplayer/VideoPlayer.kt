package com.example.composevideoplayer

import androidx.annotation.RawRes
import androidx.compose.*
import androidx.ui.core.Alignment
import androidx.ui.core.ContextAmbient
import androidx.ui.core.Modifier
import androidx.ui.foundation.ContentColorAmbient
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.*
import androidx.ui.material.Scaffold

val VideoPlayerControllerAmbient = ambientOf<VideoPlayerController> { error("VideoPlayerController is not initialized") }

sealed class VideoPlayerSource {
    data class Raw(@RawRes val resId: Int) : VideoPlayerSource()
    data class Network(val url: String, val headers: Map<String, String> = mapOf()) : VideoPlayerSource()
}

@Composable
fun VideoPlayer(
        source: VideoPlayerSource,
        backgroundColor: Color = Color.Black,
        gesturesEnabled: Boolean = true,
        modifier: Modifier = Modifier
): MediaPlaybackControls {
    val context = ContextAmbient.current
    val controller = remember {
        VideoPlayerController(context, source)
    }

    onPreCommit(source) {
        controller.setSource(source)
    }

    onPreCommit(gesturesEnabled) {
        controller.enableGestures(gesturesEnabled)
    }

    onCommit(backgroundColor) {
        controller.playerViewBackgroundColor = backgroundColor
    }

    val videoSize by controller.videoSize.collectAsState()

    Providers(
            ContentColorAmbient provides Color.White,
            VideoPlayerControllerAmbient provides controller
    ) {
        Stack(modifier = Modifier.fillMaxWidth()
                .drawBackground(color = backgroundColor)
                .aspectRatio(videoSize.width / videoSize.height)
                + modifier) {

            PlayerSurface(modifier = Modifier.gravity(Alignment.Center))
            MediaControlGestures(modifier = Modifier.matchParentSize())
            MediaControlButtons(modifier = Modifier.matchParentSize())
            ProgressIndicator(modifier = Modifier.gravity(Alignment.BottomCenter))
//            AndroidSeekBar(modifier = Modifier.gravity(Alignment.BottomCenter))
        }
    }

    onDispose {
        controller.onDispose()
    }

    return controller
}