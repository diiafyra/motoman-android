package io.github.diiafyra.motoman.ui.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * App theme. Uses the wallpaper-based dynamic colors on Android 12 and above, and the fallback
 * schemes in [Color.kt] below that.
 */
@Composable
fun MotoManTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDynamicColor = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme =
        when {
            useDynamicColor && darkTheme -> dynamicDarkColorScheme(context)
            useDynamicColor -> dynamicLightColorScheme(context)
            darkTheme -> DarkColors
            else -> LightColors
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MotoManTypography,
        content = content,
    )
}

private val LightColors =
    lightColorScheme(
        primary = Teal40,
        secondary = Slate40,
        tertiary = Amber40,
    )

private val DarkColors =
    darkColorScheme(
        primary = Teal80,
        secondary = Slate80,
        tertiary = Amber80,
    )
