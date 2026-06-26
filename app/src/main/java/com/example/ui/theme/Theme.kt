package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = NaturalDarkPrimary,
    secondary = NaturalSecondary,
    tertiary = NaturalTerracotta,
    background = NaturalDarkBackground,
    surface = NaturalDarkSurface,
    onPrimary = NaturalDarkBackground,
    onSecondary = NaturalDarkOnBackground,
    onBackground = NaturalDarkOnBackground,
    onSurface = NaturalDarkOnSurface,
    outlineVariant = NaturalDarkOutlineVariant
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NaturalPrimary,
    primaryContainer = NaturalPrimaryContainer,
    onPrimaryContainer = NaturalOnPrimaryContainer,
    secondary = NaturalSecondary,
    secondaryContainer = NaturalSecondaryContainer,
    tertiary = NaturalTerracotta,
    background = NaturalBackground,
    surface = NaturalSurface,
    onPrimary = NaturalSurface,
    onSecondary = NaturalOnPrimaryContainer,
    onBackground = NaturalOnBackground,
    onSurface = NaturalOnSurface,
    onSurfaceVariant = NaturalOnSurfaceVariant,
    outlineVariant = NaturalOutlineVariant
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Disable default dynamic color to show our custom organic garden colors!
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
