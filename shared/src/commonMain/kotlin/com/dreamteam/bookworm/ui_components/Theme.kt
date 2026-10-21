package com.dreamteam.bookworm.ui_components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Colour tokens taken from the BookWorm design canvas. */
object BookWormColors {
    val Background = Color(0xFFFFF9EE)
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1B2440)
    val InkMuted = Color(0xFF4A5272)

    val Border = Color(0xFFE3D9C0)
    val Divider = Color(0xFFEFE6D2)
    val Track = Color(0xFFE6DFCE)
    val TrackShadow = Color(0xFFCFC6AF)
    val Outline = Color(0xFF8C8568)
    val LockedIcon = Color(0xFF5E583E)

    val Primary = Color(0xFF4B3FD6)
    val PrimaryDark = Color(0xFF3328A8)
    val PrimaryDeep = Color(0xFF2A2090)
    val PrimarySoft = Color(0xFFE9E6FF)
    val PrimaryTint = Color(0xFFF4F2FF)

    val Success = Color(0xFF14795A)
    val SuccessDark = Color(0xFF0C5A41)
    val SuccessSoft = Color(0xFFDDF3EA)

    val Danger = Color(0xFFB3321F)
    val DangerDark = Color(0xFF8C2415)
    val DangerSoft = Color(0xFFFDE3DE)

    val Sun = Color(0xFFFFB627)
    val SunDark = Color(0xFFD98E00)
    val SunSoft = Color(0xFFFFEFC7)
    val Flame = Color(0xFFF26B1D)
}

/**
 * The canvas uses Fredoka for headings and buttons and Nunito for body text.
 * Drop their font files into composeResources/font and point these at them.
 */
val DisplayFontFamily: FontFamily = FontFamily.Default
val BodyFontFamily: FontFamily = FontFamily.Default

private val BookWormTypography = Typography(
    displaySmall = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 39.sp),
    headlineLarge = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 37.sp),
    headlineMedium = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 23.sp),
    titleSmall = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = 0.04.em),
    labelMedium = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
    labelSmall = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 0.1.em),
)

private val BookWormShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val BookWormColorScheme = lightColorScheme(
    primary = BookWormColors.Primary,
    onPrimary = Color.White,
    primaryContainer = BookWormColors.PrimarySoft,
    onPrimaryContainer = BookWormColors.PrimaryDark,
    secondary = BookWormColors.Success,
    onSecondary = Color.White,
    secondaryContainer = BookWormColors.SuccessSoft,
    onSecondaryContainer = BookWormColors.SuccessDark,
    tertiary = BookWormColors.Sun,
    onTertiary = BookWormColors.Ink,
    tertiaryContainer = BookWormColors.SunSoft,
    onTertiaryContainer = BookWormColors.Ink,
    error = BookWormColors.Danger,
    onError = Color.White,
    errorContainer = BookWormColors.DangerSoft,
    onErrorContainer = BookWormColors.DangerDark,
    background = BookWormColors.Background,
    onBackground = BookWormColors.Ink,
    surface = BookWormColors.Surface,
    onSurface = BookWormColors.Ink,
    surfaceVariant = BookWormColors.Track,
    onSurfaceVariant = BookWormColors.InkMuted,
    outline = BookWormColors.Border,
    outlineVariant = BookWormColors.Divider,
)

@Composable
fun BookWormTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BookWormColorScheme,
        typography = BookWormTypography,
        shapes = BookWormShapes,
        content = content,
    )
}
