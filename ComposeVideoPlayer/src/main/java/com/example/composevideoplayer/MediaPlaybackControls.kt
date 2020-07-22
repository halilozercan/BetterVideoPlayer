package com.example.composevideoplayer

interface MediaPlaybackControls {

    fun play()

    fun setSource(source: VideoPlayerSource)

    fun pause()

    fun playPauseToggle()

    fun quickSeekForward()

    fun quickSeekBackward()

    fun seekTo(position: Long)
}