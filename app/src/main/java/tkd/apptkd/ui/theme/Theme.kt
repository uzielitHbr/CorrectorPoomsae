package tkd.apptkd.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColors(
    val resultCorrect: Color,
    val resultImprovable: Color,
    val resultIncorrect: Color,
    val resultNotEvaluable: Color,
    val overlayScrim: Color,
)

private val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        resultCorrect = ResultCorrectLight,
        resultImprovable = ResultImprovableLight,
        resultIncorrect = ResultIncorrectLight,
        resultNotEvaluable = ResultNotEvaluableLight,
        overlayScrim = OverlayScrim,
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurface = OnSurfaceDark,
    outline = OutlineDark,
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurface = OnSurfaceLight,
    outline = OutlineLight,
)

private val DarkExtendedColors = ExtendedColors(
    resultCorrect = ResultCorrectDark,
    resultImprovable = ResultImprovableDark,
    resultIncorrect = ResultIncorrectDark,
    resultNotEvaluable = ResultNotEvaluableDark,
    overlayScrim = OverlayScrim,
)

private val LightExtendedColors = ExtendedColors(
    resultCorrect = ResultCorrectLight,
    resultImprovable = ResultImprovableLight,
    resultIncorrect = ResultIncorrectLight,
    resultNotEvaluable = ResultNotEvaluableLight,
    overlayScrim = OverlayScrim,
)

object TaegeukTheme {
    val extendedColors: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
}

@Composable
fun TaegeukCoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
