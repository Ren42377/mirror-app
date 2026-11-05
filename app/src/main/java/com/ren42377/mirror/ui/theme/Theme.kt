package com.ren42377.mirror.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DefaultDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8CD0E3),
    onPrimary = Color(0xFF003542),
    primaryContainer = Color(0xFF004E5F),
    onPrimaryContainer = Color(0xFFB6EBFF),
    secondary = Color(0xFFB3CAD3),
    onSecondary = Color(0xFF1E333A),
    secondaryContainer = Color(0xFF354A52),
    onSecondaryContainer = Color(0xFFCFE6F0),
    tertiary = Color(0xFFC6C2EA),
    onTertiary = Color(0xFF2F2D4D),
    tertiaryContainer = Color(0xFF454364),
    onTertiaryContainer = Color(0xFFE3DFFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1416),
    onBackground = Color(0xFFDEE3E6),
    surface = Color(0xFF0F1416),
    onSurface = Color(0xFFDEE3E6),
    surfaceVariant = Color(0xFF40484B),
    onSurfaceVariant = Color(0xFFBFC8CC),
    surfaceContainerLowest = Color(0xFF0A0F11),
    surfaceContainerLow = Color(0xFF171C1F),
    surfaceContainer = Color(0xFF1B2023),
    surfaceContainerHigh = Color(0xFF252B2D),
    surfaceContainerHighest = Color(0xFF303638),
    outline = Color(0xFF899296),
    outlineVariant = Color(0xFF40484B)
)

private val DefaultLightColorScheme = lightColorScheme(
    primary = Color(0xFF00677D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB6EBFF),
    onPrimaryContainer = Color(0xFF001F27),
    secondary = Color(0xFF4C626A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE6F0),
    onSecondaryContainer = Color(0xFF071E25),
    tertiary = Color(0xFF5D5B7D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE3DFFF),
    onTertiaryContainer = Color(0xFF191836),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5FAFC),
    onBackground = Color(0xFF171C1F),
    surface = Color(0xFFF5FAFC),
    onSurface = Color(0xFF171C1F),
    surfaceVariant = Color(0xFFDBE4E8),
    onSurfaceVariant = Color(0xFF40484B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF4F7),
    surfaceContainer = Color(0xFFE9EFF1),
    surfaceContainerHigh = Color(0xFFE3E9EC),
    surfaceContainerHighest = Color(0xFFDEE3E6),
    outline = Color(0xFF70787C),
    outlineVariant = Color(0xFFBFC8CC)
)

private val MirrorShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val MirrorTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
)

@Composable
fun MirrorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DefaultDarkColorScheme
        else -> DefaultLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = MirrorShapes,
        typography = MirrorTypography,
        content = content
    )
}
