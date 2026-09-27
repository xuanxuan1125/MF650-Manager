package com.mf650.manager.ui.theme

import androidx.compose.ui.graphics.Color

// Unified Semantic Accents
val AccentCyan = Color(0xFF00E5FF)
val AccentCyanDark = Color(0xFF00B0FF)
val AccentTeal = Color(0xFF00BFA5)
val AccentBlue = Color(0xFF2979FF)

// Traffic Speed Indicators (Globally Uniform)
val SpeedDownload = Color(0xFF00E5FF) // Cyan
val SpeedUpload = Color(0xFFFF4081)   // Magenta

// Status & Signal Indicators
val StatusSuccess = Color(0xFF00E676)
val StatusWarning = Color(0xFFFFB300)
val StatusDanger = Color(0xFFFF3D00)
val StatusNeutral = Color(0xFF90A4AE)

// Risk Levels (R0..R4)
val RiskColorR0 = Color(0xFF00E676)
val RiskColorR1 = Color(0xFF00B0FF)
val RiskColorR2 = Color(0xFFFFB300)
val RiskColorR3 = Color(0xFFFF5252)
val RiskColorR4 = Color(0xFFD50000)

// Backward-compatible semantic aliases
val CyanPrimary = AccentCyan
val SignalExcellent = StatusSuccess
val SignalGood = StatusSuccess
val SignalFair = StatusWarning
val SignalPoor = StatusDanger
val RiskR0 = RiskColorR0
val RiskR1 = RiskColorR1
val RiskR2 = RiskColorR2
val RiskR3 = RiskColorR3
val RiskR4 = RiskColorR4
val SignalUnknown = StatusNeutral
val OnCyanPrimary = Color(0xFF001F29)
val CyanPrimaryContainer = Color(0xFF004D5A)
val CyanPrimaryDark = AccentCyanDark

// Light Theme Palette
val LightBg = Color(0xFFF4F6FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8EEF5)
val LightBorder = Color(0x332B3A4A)
val LightTextPrimary = Color(0xFF1A202C)
val LightTextSecondary = Color(0xFF4A5568)

// Dark Theme Palette
val DarkBg = Color(0xFF0C1017)
val DarkSurface = Color(0xFF151C26)
val DarkSurfaceVariant = Color(0xFF1D2634)
val DarkBorder = Color(0x338FA0B8)
val DarkTextPrimary = Color(0xFFF7FAFC)
val DarkTextSecondary = Color(0xFFA0AEC0)

// AMOLED True Black Palette
val AmoledBg = Color(0xFF000000)
val AmoledSurface = Color(0xFF0A0C10)
val AmoledSurfaceVariant = Color(0xFF12151C)
val AmoledBorder = Color(0x33445566)
val AmoledTextPrimary = Color(0xFFFFFFFF)
val AmoledTextSecondary = Color(0xFF8899A6)

// Theme background aliases
val DarkBackground = DarkBg
val LightBackground = LightBg
