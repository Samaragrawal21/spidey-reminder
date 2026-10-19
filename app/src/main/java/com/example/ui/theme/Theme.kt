package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SpideyDarkColorScheme = darkColorScheme(
    primary = SpideyCrimson,
    onPrimary = WebWhite,
    primaryContainer = SpideyCrimsonDark,
    onPrimaryContainer = WebWhite,
    secondary = StatusSnoozedOrange,
    onSecondary = MatteBlack,
    tertiary = StatusCompletedGreen,
    onTertiary = MatteBlack,
    background = MatteBlack,
    onBackground = WebWhite,
    surface = DarkSlate,
    onSurface = WebWhite,
    surfaceVariant = ElevatedSlate,
    onSurfaceVariant = MetadataGrey,
    outline = SubtleBorderGrey,
    error = SpideyCrimson,
    onError = WebWhite
)

@Composable
fun SpideyReminderTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SpideyDarkColorScheme,
        typography = Typography,
        content = content
    )
}
