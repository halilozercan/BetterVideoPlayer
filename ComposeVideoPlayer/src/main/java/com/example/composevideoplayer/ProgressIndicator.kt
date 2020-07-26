package com.example.composevideoplayer

import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.compose.*
import androidx.lifecycle.lifecycleScope
import androidx.ui.core.DensityAmbient
import androidx.ui.core.LifecycleOwnerAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.dragGestureFilter
import androidx.ui.core.gesture.scrollorientationlocking.Orientation
import androidx.ui.core.onPositioned
import androidx.ui.foundation.Icon
import androidx.ui.foundation.Text
import androidx.ui.foundation.gestures.draggable
import androidx.ui.geometry.Offset
import androidx.ui.layout.*
import androidx.ui.material.LinearProgressIndicator
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.PinDrop
import androidx.ui.unit.IntSize
import androidx.ui.unit.dp
import androidx.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch

@Composable
fun ProgressIndicator(
        modifier: Modifier = Modifier
) {
    val controller = VideoPlayerControllerAmbient.current
    val progress by controller.currentPosition.collectAsState()
    val max by controller.duration.collectAsState()
    val controlsVisible by controller.controlsVisible.collectAsState()

    var wasPlaying by state { false }

    SeekBar(
            progress = progress,
            max = max,
            enabled = controlsVisible,
            onSeekStarted = {
                wasPlaying = controller.isPlaying.value
                controller.pause()
            },
            onSeek = {

            },
            onSeekStopped = {
                controller.seekTo(it)
                if(wasPlaying) {
                    controller.play()
                }
            },
            modifier = modifier
    )
}

@Composable
fun AndroidSeekBar(
        modifier: Modifier = Modifier
) {
    val controller = VideoPlayerControllerAmbient.current

    var seekBar: AppCompatSeekBar? = null

    val seekBarUpdateJob = LifecycleOwnerAmbient.current.lifecycleScope.launchWhenResumed {
        launch {
            controller.currentPosition.collect {
                seekBar?.progress = it.toInt()
            }
        }

        launch {
            controller.duration.collect {
                seekBar?.max = it.toInt()
            }
        }
    }

    onDispose {
        seekBarUpdateJob.cancel()
    }

    Column(modifier = Modifier + modifier) {
        Row(
                modifier = Modifier.fillMaxWidth()
        ) {
            AndroidView(
                    resId = R.layout.seekbar,
                    modifier = Modifier.weight(1f).offset(y = (4).dp)
            ) {
                seekBar = it.findViewById(R.id.seek_bar)
                seekBar?.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                    var wasPlaying: Boolean = true
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        Log.d("SeekBar", "$progress $fromUser ${seekBar.max} ${seekBar.progress}")
                        if (fromUser) {
                            controller.seekTo(progress.toLong())
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                        wasPlaying = controller.isPlaying.value
                        if (wasPlaying) controller.pause() // keeps the time updater running, unlike pause()
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        if (wasPlaying) controller.play()
                    }
                })
            }
        }
    }
}