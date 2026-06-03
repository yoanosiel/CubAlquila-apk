package com.example.ui.theme

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFFD0BCFF),
  secondary = Color(0xFFCCC2DC),
  tertiary = Color(0xFF4F378B),
  background = Color(0xFF141218),
  surface = Color(0xFF25232A),
  onPrimary = Color(0xFF381E72),
  onSecondary = Color(0xFF332D41),
  onBackground = Color(0xFFE6E1E5),
  onSurface = Color(0xFFE6E1E5),
  surfaceVariant = Color(0xFF49454F),
  onSurfaceVariant = Color(0xFFCAC4D0),
  outline = Color(0xFF938F99),
)

private val LightColorScheme = lightColorScheme(
  primary = PolishPrimary,
  secondary = PolishSecondary,
  tertiary = PolishPrimaryContainer,
  background = PolishBackground,
  surface = PolishSurface,
  onPrimary = PolishOnPrimary,
  onSecondary = PolishOnSecondary,
  onBackground = PolishOnBackground,
  onSurface = PolishOnSurface,
  surfaceVariant = PolishSurfaceVariant,
  onSurfaceVariant = PolishOnSurfaceVariant,
  outline = PolishOutline,
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
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
