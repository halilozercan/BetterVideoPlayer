package com.example.composevideoplayer

import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.foundation.Icon
import androidx.ui.graphics.Color
import androidx.ui.graphics.vector.VectorAsset
import androidx.ui.layout.Stack
import androidx.ui.layout.offset
import androidx.ui.layout.size
import androidx.ui.unit.dp

@Composable
fun ShadowedIcon(
        icon: VectorAsset,
        modifier: Modifier = Modifier
) {
    Stack    {
        Icon(
                asset = icon.copy(defaultWidth = 32.dp, defaultHeight = 32.dp),
                tint = Color.Black.copy(alpha = 0.3f),
                modifier = Modifier.offset(2.dp, 2.dp) + modifier
        )
        Icon(
                asset = icon.copy(defaultWidth = 32.dp, defaultHeight = 32.dp),
                modifier = Modifier + modifier
        )
    }
}