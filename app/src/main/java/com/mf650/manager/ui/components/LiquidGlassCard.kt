package com.mf650.manager.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mf650.manager.ui.theme.GlassTokens

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = GlassTokens.RadiusMedium,
    contentPadding: Dp = GlassTokens.ScreenPadding,
    onClick: (() -> Unit)? = null,
    borderBrush: Brush? = null,
    backgroundBrush: Brush? = null,
    elevation: Dp = GlassTokens.ElevationLow,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.985f else 1f,
        label = "glassCardScale"
    )

    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline

    val defaultBgBrush = backgroundBrush ?: Brush.verticalGradient(
        colors = listOf(
            surfaceColor.copy(alpha = GlassTokens.AlphaGlassHigh),
            surfaceColor.copy(alpha = GlassTokens.AlphaGlassBalanced)
        )
    )

    val defaultBorderBrush = borderBrush ?: Brush.linearGradient(
        colors = listOf(
            outlineColor.copy(alpha = GlassTokens.AlphaBorderHighlight),
            outlineColor.copy(alpha = GlassTokens.AlphaBorderLight)
        )
    )

    Box(
        modifier = modifier
            .scale(animatedScale)
            .shadow(elevation, shape, ambientColor = Color.Black.copy(alpha = 0.08f))
            .clip(shape)
            .background(defaultBgBrush)
            .border(GlassTokens.BorderWidth, defaultBorderBrush, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(contentPadding),
        content = content
    )
}
