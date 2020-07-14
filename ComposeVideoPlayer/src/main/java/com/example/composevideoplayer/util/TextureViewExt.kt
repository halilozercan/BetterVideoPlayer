package com.example.composevideoplayer.util

import android.graphics.Matrix
import android.view.TextureView

internal fun TextureView.adjustAspectRatio(viewWidth: Int, viewHeight: Int, videoWidth: Int, videoHeight: Int) {
    val aspectRatio = videoHeight.toDouble() / videoWidth
    val newWidth: Int
    val newHeight: Int

    if (viewHeight > (viewWidth * aspectRatio).toInt()) {
        // limited by narrow width; restrict height
        newWidth = viewWidth
        newHeight = (viewWidth * aspectRatio).toInt()
    } else {
        // limited by short height; restrict width
        newWidth = (viewHeight / aspectRatio).toInt()
        newHeight = viewHeight
    }

    val xoff = (viewWidth - newWidth) / 2
    val yoff = (viewHeight - newHeight) / 2

    val txform = Matrix()
    getTransform(txform)
    txform.setScale(newWidth.toFloat() / viewWidth, newHeight.toFloat() / viewHeight)
    txform.postTranslate(xoff.toFloat(), yoff.toFloat())
    setTransform(txform)
}