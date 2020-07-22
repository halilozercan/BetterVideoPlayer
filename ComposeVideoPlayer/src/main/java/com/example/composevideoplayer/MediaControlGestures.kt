package com.example.composevideoplayer

import android.util.Log
import androidx.compose.Composable
import androidx.compose.collectAsState
import androidx.compose.getValue
import androidx.lifecycle.lifecycleScope
import androidx.ui.core.Alignment
import androidx.ui.core.LifecycleOwnerAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.doubleTapGestureFilter
import androidx.ui.core.gesture.dragGestureFilter
import androidx.ui.core.gesture.tapGestureFilter
import androidx.ui.core.onPositioned
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Shadow
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxSize
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontWeight
import androidx.ui.unit.IntSize
import androidx.ui.unit.TextUnit
import com.example.composevideoplayer.util.getDurationString
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun MediaControlGestures(
        onClick: () -> Unit = {},
        modifier: Modifier = Modifier
) {
    val controller = VideoPlayerControllerAmbient.current
    val lifecycleOwnerAmbient = LifecycleOwnerAmbient.current

    lateinit var boxSize: IntSize

    val dragObserver = object: DragObserver {
        var wasPlaying: Boolean = true
        var totalOffset = Offset.Zero
        var diffTime = -1f

        var duration: Long = 0
        var currentPosition: Long = 0

        // When this job completes, it seeks to desired position.
        // It gets cancelled if delay does not complete
        var seekJob: Job? = null

        fun resetState() {
            totalOffset = Offset.Zero
            controller.draggingProgressText.value = null
        }

        override fun onStart(downPosition: Offset) {
            wasPlaying = controller.isPlaying.value
            controller.pause()

            currentPosition = controller.currentPosition.value
            duration = controller.duration.value

            resetState()
        }

        override fun onStop(velocity: Offset) {
            if(wasPlaying) controller.play()
            resetState()
        }

        override fun onDrag(dragDistance: Offset): Offset {
            seekJob?.cancel()

            totalOffset += dragDistance

            val diff = totalOffset.x

            diffTime = if (duration <= 60_000) {
                duration.toFloat() * diff / boxSize.width.toFloat()
            } else {
                60_000.toFloat() * diff / boxSize.width.toFloat()
            }

            var finalTime = currentPosition + diffTime
            if (finalTime < 0) {
                finalTime = 0f
            } else if (finalTime > duration) {
                finalTime = duration.toFloat()
            }
            diffTime = finalTime - currentPosition

            val progressText = "${getDurationString(finalTime.toLong(), false)} " +
                    "[${if(diffTime < 0) "-" else "+"}${getDurationString(abs(diffTime.toLong()), false)}]"
            controller.draggingProgressText.value = progressText

            seekJob = lifecycleOwnerAmbient.lifecycleScope.launch {
                delay(200)

                controller.seekTo(finalTime.toLong())
            }

            return dragDistance
        }
    }

    Stack(modifier = Modifier + modifier) {
        Box(
                modifier = Modifier
                        .fillMaxSize()
                        .onPositioned {
                            boxSize = it.size
                        }
                        .dragGestureFilter(dragObserver = dragObserver)
                        .tapGestureFilter {
                            onClick()
                        }
                        .doubleTapGestureFilter {
                            if(it.x < boxSize.width/2) {
                                controller.quickSeekBackward()
                            } else {
                                controller.quickSeekForward()
                            }
                        } + modifier
        )
        DraggingProgressText(modifier = Modifier.gravity(Alignment.Center))
    }

}

@Composable
fun DraggingProgressText(modifier: Modifier = Modifier) {
    val controller = VideoPlayerControllerAmbient.current

    val draggingProgressText by controller.draggingProgressText.collectAsState()

    val draggingProgressTextString = draggingProgressText

    if(draggingProgressTextString != null) {
        Text(draggingProgressTextString,
                fontSize = TextUnit.Companion.Sp(26),
                fontWeight = FontWeight.Bold,
                style = TextStyle(shadow = Shadow(
                        blurRadius = 8f,
                        offset = Offset(2f,2f))
                ),
                modifier = Modifier + modifier
        )
    }

}

