package com.example.geofencegroups.ui.theme

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
import androidx.compose.ui.unit.sp

/** Orbit colour tokens: warm paper, deep forest ink, one restrained lime accent. */
object Orbit {
    val Paper = Color(0xFFF7F3EA)
    val Surface = Color(0xFFFFFCF5)
    val Sand = Color(0xFFECE6D6)
    val Line = Color(0xFFDDD6C3)
    val Forest = Color(0xFF14382B)
    val ForestSoft = Color(0xFF55695E)
    val Moss = Color(0xFF2F5D49)
    val Lime = Color(0xFFB9E34B)
    val LimeSoft = Color(0xFFE8F3BE)
    val Ember = Color(0xFFB9420E)
    val EmberSoft = Color(0xFFFAE2D2)
    val Amber = Color(0xFF8A6100)
    val AmberSoft = Color(0xFFF6E9C6)
}

object Space {
    val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 16.dp; val xl = 24.dp; val xxl = 32.dp
}

private val Display = FontFamily.Serif
private val Body = FontFamily.SansSerif

private val OrbitTypography = Typography(
    displayMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Body, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Body, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
)

private val OrbitShapes = Shapes(
    small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(22.dp),
)

private val Scheme = lightColorScheme(
    primary = Orbit.Forest, onPrimary = Orbit.Paper,
    primaryContainer = Orbit.LimeSoft, onPrimaryContainer = Orbit.Forest,
    secondary = Orbit.Lime, onSecondary = Orbit.Forest,
    background = Orbit.Paper, onBackground = Orbit.Forest,
    surface = Orbit.Surface, onSurface = Orbit.Forest,
    surfaceVariant = Orbit.Sand, onSurfaceVariant = Orbit.ForestSoft,
    outline = Orbit.Line, outlineVariant = Orbit.Line,
    error = Orbit.Ember, onError = Orbit.Paper, errorContainer = Orbit.EmberSoft, onErrorContainer = Orbit.Ember,
)

@Composable
fun OrbitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = OrbitTypography, shapes = OrbitShapes, content = content)
}
