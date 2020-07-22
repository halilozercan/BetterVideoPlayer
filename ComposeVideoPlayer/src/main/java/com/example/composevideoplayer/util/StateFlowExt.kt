package com.example.composevideoplayer.util

import androidx.compose.MutableState
import com.example.composevideoplayer.VideoPlayerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

@ExperimentalCoroutinesApi
fun MutableStateFlow<VideoPlayerState>.set(block: VideoPlayerState.() -> VideoPlayerState) {
    this.value = this.value.block()
}