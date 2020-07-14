package com.example.composevideoplayer

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.Composable
import androidx.ui.viewinterop.AndroidView
import com.example.composevideoplayer.util.adjustAspectRatio

@Composable
fun PlayerSurface(
        playerSurfaceCallback: PlayerSurfaceCallback
): PlayerSurfaceController {

    var textureViewWidth = 0
    var textureViewHeight = 0

    var videoWidth = 0
    var videoHeight = 0

    var textureView: TextureView? = null

    AndroidView(resId = R.layout.surface) {
        textureView = it.findViewById<TextureView>(R.id.texture_view)

        textureView?.surfaceTextureListener = object: TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                textureViewWidth = width
                textureViewHeight = height

                textureView?.adjustAspectRatio(width, height, videoWidth, videoHeight)
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {

            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                textureViewWidth = width
                textureViewHeight = height

                playerSurfaceCallback.surfaceAvailable(surfaceTexture, width, height)
            }

        }
    }

    return object: PlayerSurfaceController {
        override fun videoSizeChanged(width: Int, height: Int) {
            videoWidth = width
            videoHeight = height

            textureView?.adjustAspectRatio(textureViewWidth, textureViewHeight, videoWidth, videoHeight)
        }

    }
}

interface PlayerSurfaceController {

    fun videoSizeChanged(width: Int, height: Int)

}

interface PlayerSurfaceCallback {

    fun surfaceAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int)

}