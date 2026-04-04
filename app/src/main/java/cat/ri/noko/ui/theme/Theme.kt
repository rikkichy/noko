package cat.ri.noko.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cat.ri.noko.core.SettingsManager

val NokoFieldShape = RoundedCornerShape(20.dp)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun nokoTopAppBarColors(): TopAppBarColors =
    TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun NokoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val amoled by SettingsManager.amoledMode.collectAsState(initial = SettingsManager.isAmoled())

    val colors: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> if (darkTheme) darkColorScheme() else expressiveLightColorScheme()
    }.let { scheme ->
        if (amoled && darkTheme) {
            scheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceTint = Color.Transparent,
                surfaceContainer = Color(0xFF0A0A0A),
                surfaceContainerLow = Color(0xFF050505),
                surfaceContainerHigh = Color(0xFF121212),
                surfaceContainerHighest = Color(0xFF1A1A1A),
                surfaceContainerLowest = Color.Black,
            )
        } else {
            scheme.copy(surfaceTint = Color.Transparent)
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colors,
        typography = Typography(),
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
