package com.example.composedemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.*
import androidx.lifecycle.Lifecycle
import androidx.ui.core.*
import androidx.ui.foundation.Icon
import androidx.ui.foundation.Text
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
            val context = ContextAmbient.current
            val controller = VideoPlayerController(
                    context,
                    VideoPlayerSource.Raw(R.raw.video)
            )

            ComposeVideoPlayerTheme {
                Column(verticalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxHeight()) {
                    Text("Welcome to",
                            fontSize = TextUnit.Sp(36),
                            color = Color.Blue,
                            style = TextStyle(
                                    textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.fillMaxWidth()
                    )

                    VideoPlayer(controller)

                    Text("Jetpack Compose",
                            fontSize = TextUnit.Sp(36),
                            color = Color.Blue,
                            style = TextStyle(
                                    textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.fillMaxWidth()
                    )

                    /*Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = {
                        controller.setSource(VideoPlayerSource.Network("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"))
                    }, modifier = Modifier.weight(1f)) {
                        Text("Go")
                    }*/
                }
            }
        }
    }
}

@Composable
fun foo() {
    val playButtonUiState = state { true }
    IconButton(
            onClick = {
                playButtonUiState.value = !playButtonUiState.value
            }
    ) {
        Icon(
                asset = if (playButtonUiState.value) { Icons.Filled.PlayArrow } else { Icons.Filled.Star },
                modifier = Modifier/*.drawShadow(elevation = 2.dp)*/
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    foo()
}