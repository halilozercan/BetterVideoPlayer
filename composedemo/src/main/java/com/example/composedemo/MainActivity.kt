package com.example.composedemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.*
import androidx.lifecycle.Lifecycle
import androidx.ui.core.*
import androidx.ui.foundation.Icon
import androidx.ui.foundation.ScrollableColumn
import androidx.ui.foundation.Text
import androidx.ui.foundation.clickable
import androidx.ui.graphics.Color
import androidx.ui.layout.*
import androidx.ui.material.Button
import androidx.ui.material.IconButton
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.PlayArrow
import androidx.ui.material.icons.filled.Star
import androidx.ui.material.icons.lazyMaterialIcon
import androidx.ui.material.icons.materialPath
import androidx.ui.text.TextStyle
import androidx.ui.text.style.TextAlign
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.TextUnit
import androidx.ui.unit.dp
import com.example.composedemo.ui.ComposeVideoPlayerTheme
import com.example.composevideoplayer.VideoPlayer
import com.example.composevideoplayer.VideoPlayerController
import com.example.composevideoplayer.VideoPlayerSource

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val (backgroundColorState, setBackgroundColorState) = state { Color.Black }
            val (sourceState, setSourceState) = state { VideoPlayerSource.Network(Urls.bigBuckBunny) }
            val (gesturesEnabled, setGesturesEnabled) = state { true }

            ComposeVideoPlayerTheme {
                ScrollableColumn(modifier = Modifier.fillMaxHeight()) {
                    val mediaPlaybackControls = VideoPlayer(
                            sourceState,
                            backgroundColorState,
                            gesturesEnabled
                    )

                    Row(modifier = Modifier.padding(vertical = 16.dp)) {
                        Button(onClick = {
                            setSourceState(VideoPlayerSource.Network(Urls.bigBuckBunny))
                        }) {
                            Text("Big Buck Bunny")
                        }

                        Button(onClick = {
                            setSourceState(VideoPlayerSource.Network(Urls.alice))
                        }) {
                            Text("Alice")
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Text("Default")
}

object Urls {

    val bigBuckBunny = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    val alice = "http://www.exit109.com/~dnn/clips/RW20seconds_1.mp4"

}