package com.reevan.reevzdrip.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary = SlateInk,
    onPrimary = StoneCard,
    primaryContainer = StoneFill,
    onPrimaryContainer = SlateInk,
    secondary = SlateInkDim,
    onSecondary = StoneCard,
    secondaryContainer = StoneLineSoft,
    onSecondaryContainer = SlateInk,
    tertiary = SlateInkDim,
    onTertiary = StoneCard,
    background = StoneGround,
    onBackground = SlateInk,
    surface = StoneCard,
    onSurface = SlateInk,
    surfaceVariant = StoneFill,
    onSurfaceVariant = SlateInkDim,
    surfaceContainerLowest = StoneCard,
    surfaceContainerLow = StoneGround,
    surfaceContainer = StoneFill,
    surfaceContainerHigh = StoneLineSoft,
    surfaceContainerHighest = StoneLineSoft,
    outline = StoneLine,
    outlineVariant = StoneLineSoft,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

private val DarkScheme = darkColorScheme(
    primary = PaperInk,
    onPrimary = CharcoalGround,
    primaryContainer = CharcoalFill,
    onPrimaryContainer = PaperInk,
    secondary = PaperInkDim,
    onSecondary = CharcoalGround,
    secondaryContainer = CharcoalLineSoft,
    onSecondaryContainer = PaperInk,
    tertiary = PaperInkDim,
    onTertiary = CharcoalGround,
    background = CharcoalGround,
    onBackground = PaperInk,
    surface = CharcoalCard,
    onSurface = PaperInk,
    surfaceVariant = CharcoalFill,
    onSurfaceVariant = PaperInkDim,
    surfaceContainerLowest = CharcoalGround,
    surfaceContainerLow = CharcoalCard,
    surfaceContainer = CharcoalFill,
    surfaceContainerHigh = CharcoalLineSoft,
    surfaceContainerHighest = CharcoalLine,
    outline = CharcoalLine,
    outlineVariant = CharcoalLineSoft,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
)

/**
 * The app theme.
 *
 * **Material You dynamic colour is deliberately off.** A wallpaper-derived scheme would repaint
 * the app in whatever the phone's background happens to be, and this app's screens are full of
 * the user's own photographs — an unpredictable accent behind them works against the one job the
 * UI has, which is making a garment easy to recognise. There is no `dynamicColor` flag, so this
 * cannot be half-enabled by accident.
 *
 * [darkTheme] follows the system for now. Settings (Phase 8) will pass an explicit choice.
 */
@Composable
fun ReevzDripTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = Typography,
        content = content,
    )
}
