package com.example.composevideoplayer

import android.util.Log
import androidx.compose.*
import androidx.ui.core.*
import androidx.ui.core.gesture.*
import androidx.ui.core.gesture.scrollorientationlocking.Orientation
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.core.pointerinput.PointerInputModifier
import androidx.ui.foundation.*
import androidx.ui.foundation.gestures.draggable
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.layout.*
import androidx.ui.layout.RowScope.weight
import androidx.ui.material.LinearProgressIndicator
import androidx.ui.material.MaterialTheme
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.AddCircle
import androidx.ui.material.icons.filled.PinDrop
import androidx.ui.material.icons.lazyMaterialIcon
import androidx.ui.material.icons.materialPath
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.Density
import androidx.ui.unit.IntSize
import androidx.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * During seeking, [progress] does not have any affect.
 */
@Composable
fun SeekBar(
    progress: Long,
    max: Long,
    enabled: Boolean,
    onSeek: (progress: Long) -> Unit = {},
    onSeekStarted: (startedProgress: Long) -> Unit = {},
    onSeekStopped: (stoppedProgress: Long) -> Unit = {},
    seekerPopup: @Composable() () -> Unit = {},
    color: Color = MaterialTheme.colors.primary,
    modifier: Modifier = Modifier
) {
    var boxSize by state { IntSize(1, 1) }

    var onGoingDrag by state { false }
    val percentage = progress.coerceAtMost(max).toFloat() / max.coerceAtLeast(1L).toFloat()

    val indicatorOffsetStateByPercentage = Offset(percentage * boxSize.width.toFloat(), 0f)

    var indicatorOffsetStateByDrag by state {
        Offset.Zero
    }

    val finalIndicatorOffsetState = (indicatorOffsetStateByDrag + indicatorOffsetStateByPercentage).let {
        it.copy(x = it.x.coerceIn(0f, boxSize.width.toFloat()))
    }

    val assumedPercentage = if (onGoingDrag) {
        finalIndicatorOffsetState.x / boxSize.width.toFloat()
    } else {
        percentage
    }

    val indicatorSize = if (onGoingDrag) {
        24.dp
    } else {
        16.dp
    }

    Stack(modifier = Modifier + modifier.preferredHeight(indicatorSize).offset(y = indicatorSize / 2)) {
        if (enabled) {
            val (offsetDpX, offsetDpY) = with(DensityAmbient.current) {
                (finalIndicatorOffsetState.x).toDp() - indicatorSize / 2 to (finalIndicatorOffsetState.y).toDp()
            }

            Row(modifier = Modifier.matchParentSize()
                .dragGestureWithPressFilter(
                    dragObserver = object: DragObserver {
                        override fun onStart(touchPosition: Offset) {
                            indicatorOffsetStateByDrag = Offset(
                                x = (touchPosition.x - indicatorOffsetStateByPercentage.x),
                                y = indicatorOffsetStateByDrag.y
                            )
                            onGoingDrag = true
                            onSeekStarted(progress)

                            val currentProgress = (finalIndicatorOffsetState.x / boxSize.width.toFloat()) * max
                            onSeek(currentProgress.toLong())
                            Log.d("SeekBar", "onDragStarted")
                        }

                        override fun onDrag(dragDistance: Offset): Offset {
                            // continue as usual. onStart set the initial offset anyway
                            indicatorOffsetStateByDrag = Offset(
                                x = (indicatorOffsetStateByDrag.x + dragDistance.x / 2), // TODO: What the hell?
                                y = indicatorOffsetStateByDrag.y
                            )

                            val currentProgress = (finalIndicatorOffsetState.x / boxSize.width.toFloat()) * max
                            onSeek(currentProgress.toLong())

                            return super.onDrag(dragDistance)
                        }

                        override fun onStop(velocity: Offset) {
                            val newProgress = (finalIndicatorOffsetState.x / boxSize.width.toFloat()) * max
                            onSeekStopped(newProgress.toLong())
                            indicatorOffsetStateByDrag = Offset.Zero
                            onGoingDrag = false
                            Log.d("SeekBar", "onDragStopped")

                            super.onStop(velocity)
                        }
                    },
                    startDragImmediately = true,
                    orientation = Orientation.Horizontal
                )
            ) {

                Indicator(
                    modifier = Modifier
                        .offset(x = offsetDpX, y = offsetDpY)
                        .preferredSize(indicatorSize)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().gravity(Alignment.Center)
        ) {
            // TODO: something custom that can support secondary progress
            LinearProgressIndicator(
                modifier = Modifier.weight(1f).onPositioned {
                    if (boxSize != it.size) {
                        boxSize = it.size
                    }
                },
                progress = assumedPercentage,
                color = color
            )
        }
    }
}

fun Modifier.dragGestureWithPressFilter(
    dragObserver: DragObserver,
    orientation: Orientation,
    startDragImmediately: Boolean = false
): Modifier = composed {
    val glue = remember { TouchSlopDragGestureDetectorGlue() }
    glue.touchSlopDragObserver = dragObserver

    rawDragGestureFilter(
        glue.rawDragObserver,
        glue::enabledOrStarted,
        orientation
    )
        .dragSlopExceededGestureFilter(glue::enableDrag, orientation = orientation)
        .pressIndicatorGestureFilter(
            onStart = glue::startDrag,
            onStop = { glue.rawDragObserver.onStop(Offset.Zero) },
            enabled = startDragImmediately
        )
}

/**
 * Glues together the logic of RawDragGestureDetector, TouchSlopExceededGestureDetector, and
 * InterruptFlingGestureDetector.
 */
private class TouchSlopDragGestureDetectorGlue {

    lateinit var touchSlopDragObserver: DragObserver
    var started = false
    var enabled = false
    val enabledOrStarted
        get() = started || enabled

    fun enableDrag() {
        enabled = true
    }

    fun startDrag(downPosition: Offset) {
        started = true
        touchSlopDragObserver.onStart(downPosition)
    }

    val rawDragObserver: DragObserver =
        object : DragObserver {
            override fun onStart(downPosition: Offset) {
                if (!started) {
                    touchSlopDragObserver.onStart(downPosition)
                }
            }

            override fun onDrag(dragDistance: Offset): Offset {
                return touchSlopDragObserver.onDrag(dragDistance)
            }

            override fun onStop(velocity: Offset) {
                started = false
                enabled = false
                touchSlopDragObserver.onStop(velocity)
            }

            override fun onCancel() {
                started = false
                enabled = false
                touchSlopDragObserver.onCancel()
            }
        }
}

@Composable
fun Indicator(
    color: Color = MaterialTheme.colors.primary,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = Modifier + modifier) {
        val radius = size.height / 2
        drawCircle(color, radius)
    }
}