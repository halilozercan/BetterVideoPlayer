package com.example.composevideoplayer

import androidx.animation.*
import androidx.compose.*
import androidx.ui.animation.transition
import androidx.ui.core.*
import androidx.ui.core.gesture.*
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Shadow
import androidx.ui.layout.*
import androidx.ui.material.LinearProgressIndicator
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.FastForward
import androidx.ui.material.icons.filled.FastRewind
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontWeight
import androidx.ui.unit.IntSize
import androidx.ui.unit.TextUnit
import androidx.ui.unit.dp
import com.example.composevideoplayer.util.getDurationString
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.abs

@Composable
fun MediaControlGestures(
    modifier: Modifier = Modifier
) {
    val controller = VideoPlayerControllerAmbient.current

    val controlsEnabled by controller.controlsEnabled.collectAsState()
    val gesturesEnabled by controller.gesturesEnabled.collectAsState()
    val controlsVisible by controller.controlsVisible.collectAsState()

    if (controlsEnabled && !controlsVisible && gesturesEnabled) {
        Stack(modifier = Modifier + modifier) {
            GestureBox()
            QuickSeekAnimation()
            DraggingProgressOverlay(modifier = modifier)
        }
    }

}

@Composable
fun GestureBox(modifier: Modifier = Modifier) {
    val controller = VideoPlayerControllerAmbient.current

    lateinit var boxSize: IntSize

    val dragObserver = object : DragObserver {
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
            controller.draggingProgress.value = null
        }

        override fun onStart(downPosition: Offset) {
            wasPlaying = controller.isPlaying.value
            controller.pause()

            currentPosition = controller.currentPosition.value
            duration = controller.duration.value

            resetState()
        }

        override fun onStop(velocity: Offset) {
            if (wasPlaying) controller.play()
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

            controller.draggingProgress.value = DraggingProgress(
                    finalTime = finalTime,
                    diffTime = diffTime
            )

            seekJob = CoroutineScope(Dispatchers.Main).launch {
                delay(200)

                controller.seekTo(finalTime.toLong())
            }

            return dragDistance
        }
    }

    Row(modifier = Modifier.fillMaxSize()
            .onPositioned { boxSize = it.size }
            .dragGestureFilter(
                    dragObserver = dragObserver,
                    canDrag = {
                        it.name == "LEFT" || it.name == "RIGHT"
                    }
            )
            + modifier) {

        val commonModifier = Modifier.fillMaxHeight()
                .tapGestureFilter {
                    controller.showControls()
                }
        Box(
            modifier = commonModifier
                    .weight(2f)
                    .doubleTapGestureFilter {
                        controller.quickSeekRewind()
                    }
        )

        // Center where double tap does not exist
        Box(
                modifier = commonModifier
                        .weight(1f)
        )

        Box(
                modifier = commonModifier
                        .weight(2f)
                        .doubleTapGestureFilter {
                            controller.quickSeekForward()
                        }
        )
    }
}

@Composable
fun QuickSeekAnimation(
        modifier: Modifier = Modifier
) {
    val controller = VideoPlayerControllerAmbient.current

    val state by controller.quickSeekDirection.collectAsState()

    Row(modifier = Modifier + modifier) {
        Stack(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if(state.direction == QuickSeekDirection.Rewind) {
                val transitionState = transition(
                        definition = transitionDef,
                        initState = "start",
                        toState = "end",
                        onStateChangeFinished = {
                            controller.quickSeekDirection.value = QuickSeekAction.none()
                        }
                )

                val realAlpha = 1 - abs(1 - transitionState[alpha])
                ShadowedIcon(
                        Icons.Filled.FastRewind,
                        modifier = Modifier
                                .drawLayer(alpha = realAlpha)
                                .gravity(Alignment.Center)
                )
            }
        }

        Stack(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if(state.direction == QuickSeekDirection.Forward) {
                val transitionState = transition(
                        definition = transitionDef,
                        initState = "start",
                        toState = "end",
                        onStateChangeFinished = {
                            controller.quickSeekDirection.value = QuickSeekAction.none()
                        }
                )

                val realAlpha = 1 - abs(1 - transitionState[alpha])
                ShadowedIcon(
                        Icons.Filled.FastForward,
                        modifier = Modifier
                                .drawLayer(alpha = realAlpha)
                                .gravity(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun DraggingProgressOverlay(modifier: Modifier = Modifier) {
    val controller = VideoPlayerControllerAmbient.current

    val draggingProgress by controller.draggingProgress.collectAsState()

    val draggingProgressValue = draggingProgress

    if (draggingProgressValue != null) {
        val dur by controller.duration.collectAsState()

        val progress = if (dur != 0L) (draggingProgressValue.finalTime / dur.toFloat()) else 0f
        val progressPercentage = if (progress < 0) 0f else if (progress > 1) 1f else progress

        Stack(modifier = Modifier + modifier) {
            Text(draggingProgressValue.progressText,
                    fontSize = TextUnit.Companion.Sp(26),
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(shadow = Shadow(
                            blurRadius = 8f,
                            offset = Offset(2f, 2f))
                    ),
                    modifier = Modifier.gravity(Alignment.Center)
            )

            Row(modifier = Modifier.fillMaxWidth().gravity(Alignment.BottomCenter).padding(8.dp)) {
                LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = progressPercentage
                )
            }
        }
    }

}

private val alpha = FloatPropKey()
private val transitionDef = transitionDefinition {
    state("start") {
        this[alpha] = 0f
    }
    state("end") {
        this[alpha] = 2f
    }

    transition(fromState = "start", toState = "end") {
        alpha using tween(
                durationMillis = 500,
                easing = LinearEasing
        )
    }

    snapTransition("end" to "start")
}

data class DraggingProgress(
        val finalTime: Float,
        val diffTime: Float
) {
    val progressText: String
        get() = "${getDurationString(finalTime.toLong(), false)} " +
                "[${if (diffTime < 0) "-" else "+"}${getDurationString(abs(diffTime.toLong()), false)}]"
}

enum class QuickSeekDirection {
    None,
    Rewind,
    Forward
}

data class QuickSeekAction(
        val direction: QuickSeekDirection
) {
    // Each action is unique
    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(direction)
    }

    companion object {
        fun none() = QuickSeekAction(QuickSeekDirection.None)
        fun forward() = QuickSeekAction(QuickSeekDirection.Forward)
        fun rewind() = QuickSeekAction(QuickSeekDirection.Rewind)
    }
}