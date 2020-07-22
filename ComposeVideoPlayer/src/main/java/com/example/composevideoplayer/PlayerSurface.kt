package com.example.composevideoplayer

import android.view.TextureView
import androidx.compose.Composable
import androidx.compose.onActive
import androidx.compose.onDispose
import androidx.lifecycle.lifecycleScope
import androidx.ui.core.LifecycleOwnerAmbient
import androidx.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun PlayerSurface() {

    val controller = VideoPlayerControllerAmbient.current

    AndroidView(resId = R.layout.surface) { layout ->
        layout.findViewById<PlayerView>(R.id.player_view).let { playerView ->
            controller.playerViewAvailable(playerView)
        }
    }
}