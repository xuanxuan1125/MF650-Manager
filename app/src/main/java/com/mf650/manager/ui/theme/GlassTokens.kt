package com.mf650.manager.ui.theme

import androidx.compose.ui.unit.dp

enum class GlassQuality {
    HIGH,
    BALANCED,
    PERFORMANCE
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

object GlassTokens {
    // Corner Radii
    val RadiusLarge = 22.dp
    val RadiusMedium = 16.dp
    val RadiusSmall = 10.dp
    val RadiusPill = 999.dp

    // Border Width
    val BorderWidth = 1.dp
    val BorderWidthThick = 1.5.dp

    // Spacing
    val ScreenPadding = 16.dp
    val ItemSpacing = 12.dp
    val SectionSpacing = 16.dp

    // Elevation
    val ElevationNone = 0.dp
    val ElevationLow = 2.dp
    val ElevationMedium = 6.dp

    // Glass Surface Alphas
    const val AlphaGlassHigh = 0.65f
    const val AlphaGlassBalanced = 0.80f
    const val AlphaGlassPerformance = 0.95f
    const val PanelAlpha = AlphaGlassBalanced

    const val AlphaBorderLight = 0.35f
    const val AlphaBorderHighlight = 0.60f
    const val BorderAlpha = AlphaBorderLight
}
