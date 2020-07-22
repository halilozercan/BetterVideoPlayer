package com.example.composevideoplayer

import android.util.Log
import androidx.compose.Composable
import androidx.compose.collectAsState
import androidx.compose.getValue
import androidx.ui.core.*
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.doubleTapGestureFilter
import androidx.ui.core.gesture.dragGestureFilter
import androidx.ui.core.gesture.tapGestureFilter
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.foundation.clickable
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Shadow
import androidx.ui.layout.*
import androidx.ui.material.IconButton
import androidx.ui.material.LinearProgressIndicator
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Pause
import androidx.ui.material.icons.filled.PlayArrow
import androidx.ui.material.icons.filled.Restore
import androidx.ui.text.TextStyle
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.IntSize
import androidx.ui.unit.dp
import com.example.composevideoplayer.util.getDurationString
import kotlin.math.abs

@Composable
fun MediaControlButtons(
        modifier: Modifier = Modifier
) {
    val controller = VideoPlayerControllerAmbient.current

    Stack(modifier = Modifier + modifier) {
        Box(modifier = Modifier.gravity(Alignment.Center).fillMaxSize().clickable(indication = null) {
            controller.hideControls()
        })
        ProgressIndicator(modifier = Modifier.gravity(Alignment.BottomCenter))
        PlayPauseButton(modifier = Modifier.gravity(Alignment.Center))
    }
}

@Composable
fun ProgressIndicator(
        modifier: Modifier = Modifier
) {
    val controller = VideoPlayerControllerAmbient.current

    val pos by controller.currentPosition.collectAsState()
    val dur by controller.duration.collectAsState()

    val progress = if (dur != 0L) (pos.toFloat() / dur.toFloat()) else 0f
    val progressPercentage = if (progress < 0) 0f else if (progress > 1) 1f else progress

    Log.d("ProgressIndicator", "$progressPercentage")
    Column(modifier = Modifier + modifier) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp)
        ) {
            Text(getDurationString(pos, false),
                    style = TextStyle(shadow = Shadow(
                            blurRadius = 8f,
                            offset = Offset(2f,2f))
                    ))
            LinearProgressIndicator(
                    modifier = Modifier.weight(1f)
                            .gravity(Alignment.CenterVertically)
                            .padding(horizontal = 8.dp),
                    progress = progressPercentage
            )
            Text(
                    getDurationString(dur - pos, false),
                    style = TextStyle(shadow = Shadow(
                            blurRadius = 8f,
                            offset = Offset(2f,2f))
                    )
            )
        }
    }
}

@Composable
fun PlayPauseButton(modifier: Modifier = Modifier) {
    val controller = VideoPlayerControllerAmbient.current

    val isPlaying by controller.isPlaying.collectAsState()
    val playbackState by controller.playbackState.collectAsState()

    IconButton(
            onClick = { controller.playPauseToggle() },
            modifier = Modifier + modifier
    ) {
        if (isPlaying) {
            ShadowedIcon(icon = Icons.Filled.Pause)
        } else {
            if(playbackState == PlaybackState.READY) {
                ShadowedIcon(icon = Icons.Filled.PlayArrow)
            } else if(playbackState == PlaybackState.ENDED) {
                ShadowedIcon(icon = Icons.Filled.Restore)
            }
        }
    }
}