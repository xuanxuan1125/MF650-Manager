package com.mf650.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.mf650.manager.ui.theme.AccentCyan
import com.mf650.manager.ui.theme.AccentBlue

@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    val bgColor = MaterialTheme.colorScheme.background

    // Very subtle, non-intrusive ambient glow
    val ambientBrush = Brush.radialGradient(
        colors = listOf(
            AccentCyan.copy(alpha = 0.05f),
            AccentBlue.copy(alpha = 0.03f),
            Color.Transparent
        ),
        center = Offset(200f, 150f),
        radius = 900f
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .background(ambientBrush)
    ) {
        content?.invoke(this)
    }
}
