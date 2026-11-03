package in.zerotax.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val EnterpriseLightColorScheme = lightColorScheme(
    primary = EnterpriseTeal600,
    onPrimary = PureWhite,
    primaryContainer = EnterpriseTeal50,
    onPrimaryContainer = EnterpriseTeal900,
    secondary = Slate700,
    onSecondary = PureWhite,
    secondaryContainer = Slate100,
    onSecondaryContainer = Slate900,
    tertiary = IndigoStatutory,
    onTertiary = PureWhite,
    tertiaryContainer = IndigoStatutoryBg,
    onTertiaryContainer = IndigoStatutory,
    background = Slate50,
    onBackground = Slate900,
    surface = PureWhite,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate300,
    outlineVariant = Slate200,
    error = CrimsonDanger,
    onError = PureWhite,
    errorContainer = CrimsonDangerBg,
    onErrorContainer = CrimsonDanger
)

private val EnterpriseDarkColorScheme = darkColorScheme(
    primary = EnterpriseTeal500,
    onPrimary = Slate950,
    primaryContainer = EnterpriseTeal800,
    onPrimaryContainer = EnterpriseTeal100,
    secondary = Slate300,
    onSecondary = Slate900,
    secondaryContainer = Slate850,
    onSecondaryContainer = Slate100,
    tertiary = IndigoStatutory,
    onTertiary = PureWhite,
    tertiaryContainer = IndigoStatutoryBgDark,
    onTertiaryContainer = IndigoStatutoryBg,
    background = Slate950,
    onBackground = Slate50,
    surface = Slate900,
    onSurface = Slate50,
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    outlineVariant = Slate800,
    error = CrimsonDanger,
    onError = PureWhite,
    errorContainer = CrimsonDangerBgDark,
    onErrorContainer = CrimsonDangerBg
)

@Composable
fun ZeroTaxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep false for strict brand consistency in fintech
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> EnterpriseDarkColorScheme
        else -> EnterpriseLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EnterpriseTypography,
        content = content
    )
}
