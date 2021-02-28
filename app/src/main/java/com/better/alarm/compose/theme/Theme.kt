package com.better.alarm.compose.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ColoredTheme(
  colors: Colors,
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colors = colors,
    typography = themeTypography,
    shapes = shapes,
    content = content
  )
}

data class ThemeColors(
  val name: String,
  val key: String,
  val colors: Colors,
)

fun themeColors() = listOf(
  ThemeColors("Light", "light", LightColors),
  ThemeColors("Dark", "dark", DarkColors),
  ThemeColors("DeusEx", "deus_ex", DeusExColors),
  ThemeColors("Synth", "synthwave", SynthwaveColors),
  // ThemeColors("G", "g", Gulasch),
)

fun String.toColors(): Colors {
  return when (this) {
    "light", "Light" -> LightColors
    "dark", "Dark" -> DarkColors
    "deus_ex", "DeusEx" -> DeusExColors
    "synthwave", "Synth" -> SynthwaveColors
    "g", "G" -> Gulasch
    else -> SynthwaveColors
  }
}

private val shapes = Shapes(
  small = RoundedCornerShape(4.dp),
  medium = RoundedCornerShape(4.dp),
  large = RoundedCornerShape(8.dp)
)

private val LightColors = lightColors()
private val DarkColors = darkColors()

private val Gold0 = Color(0xfffff69f)
private val Gold1 = Color(0xfffdd870)
private val Gold2 = Color(0xffd0902f)
private val Gold3 = Color(0xffa15501)
private val Gold4 = Color(0xff351409)

private val DeusExColors = darkColors(
  primary = Gold2,
  primaryVariant = Gold3,
  secondary = Gold3,
  background = Color.Black,
  surface = Color.Black,
  error = Gold0,
  onPrimary = Color.DarkGray,
  onSecondary = Color.DarkGray,
  onBackground = Gold1,
  onSurface = Gold1,
  onError = Color.Black,
)

private val SynthPink = Color(0xffef9af2)
private val SynthDarkPink = Color(0xff831187)
private val SynthVibrantPurple = Color(0xff8a04ed)
private val SynthPurple = Color(0xffBB86FC)
private val SynthDarkPurple = Color(0xff240c76)
private val SynthDarkerPurple = Color(0xff332940)
private val SynthDarkestPurple = Color(0xff0c0c0c)

private val MaterialCyan = Color(0xFF03DAC6)

private val SynthwaveColors = darkColors(
  background = SynthDarkerPurple,
  surface = SynthDarkestPurple,
  onBackground = SynthPurple,
  onSurface = SynthPurple,
  primary = SynthPink,
  primaryVariant = SynthPink,
  secondary = MaterialCyan,
  error = Color(0xFFCF6679),
)

private val GYellow = Color(red = 255, green = 242, blue = 0)
private val GGreen = Color(red = 76, green = 177, blue = 34)
private val Gulasch = lightColors(
  background = GYellow,
  onBackground = GGreen,
  surface = GGreen,
  onSurface = GYellow,
  primary = GGreen,
  onPrimary = GYellow,
  secondary = GGreen,
)