// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Tokens from the design canvas: "Foundations" and "Foundations — dark theme roles".
internal val LightColors = lightColorScheme(
    primary = Color(0xFF0D6B62),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2F0EE),
    onPrimaryContainer = Color(0xFF0A524B),
    // Selected chips, nav indicator, slider tracks: teal tints, not M3's baseline purple.
    secondary = Color(0xFF0D6B62),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2F0EE),
    onSecondaryContainer = Color(0xFF0A524B),
    background = Color(0xFFF6F7F5),
    onBackground = Color(0xFF15181C),
    surface = Color.White,
    onSurface = Color(0xFF15181C),
    surfaceVariant = Color(0xFFEEF0F2),
    onSurfaceVariant = Color(0xFF545B63),
    // Neutral containers (cards, sheets, menus). M3's defaults are lilac-tinted.
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1F2F1),
    surfaceContainerHighest = Color(0xFFE9EBEA),
    outline = Color(0xFFC9CDD2),
    outlineVariant = Color(0xFFE3E5E8),
    error = Color(0xFFB42318),
    // Snackbar (design: dark surface, teal action).
    inverseSurface = Color(0xFF22262B),
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFF7FD3C8),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF6FD3C4),
    onPrimary = Color(0xFF00372F),
    primaryContainer = Color(0xFF0F4F48),
    onPrimaryContainer = Color(0xFFA8F0E4),
    secondary = Color(0xFF6FD3C4),
    onSecondary = Color(0xFF00372F),
    secondaryContainer = Color(0xFF0F4F48),
    onSecondaryContainer = Color(0xFFA8F0E4),
    background = Color(0xFF101314),
    onBackground = Color(0xFFE6E8EA),
    surface = Color(0xFF191D1F),
    onSurface = Color(0xFFE6E8EA),
    surfaceVariant = Color(0xFF23282B),
    onSurfaceVariant = Color(0xFFA9B0B6),
    surfaceContainerLowest = Color(0xFF0C0F10),
    surfaceContainerLow = Color(0xFF15191B),
    surfaceContainer = Color(0xFF191D1F),
    surfaceContainerHigh = Color(0xFF202528),
    surfaceContainerHighest = Color(0xFF2A2F32),
    outline = Color(0xFF3A4146),
    outlineVariant = Color(0xFF2A2F33),
    error = Color(0xFFFFB4AB),
    inverseSurface = Color(0xFFE6E8EA),
    inverseOnSurface = Color(0xFF15181C),
    inversePrimary = Color(0xFF0D6B62),
)

/** Warning colours (not part of the M3 scheme). Always pair with an icon and text. */
data class FolioStatusColors(val warning: Color, val onWarningContainer: Color, val warningContainer: Color)

internal val LightStatus = FolioStatusColors(Color(0xFF9A4A00), Color(0xFF6B3300), Color(0xFFFFF3E3))
internal val DarkStatus = FolioStatusColors(Color(0xFFFFB77A), Color(0xFFFFB77A), Color(0xFF3D2300))
