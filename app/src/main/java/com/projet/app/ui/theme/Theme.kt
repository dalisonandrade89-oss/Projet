package com.projet.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Material 3 com Dynamic Color (Android 12+) e esquema estático como fallback.
 * [settings] sobrepõe o fundo e/ou o destaque escolhidos pelo usuário; sem escolha (null),
 * o visual é exatamente o padrão do sistema.
 */
@Composable
fun ProjetTheme(settings: ThemeSettings = ThemeSettings(), content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val background = settings.background?.let { Color(it) }
    val accent = settings.accent?.let { Color(it) }
    // Com fundo escolhido, claro/escuro vem da luminância dele (não do modo do sistema).
    val darkTheme = background?.let { it.luminance() <= LIGHT_THRESHOLD } ?: systemDark

    val base = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    SystemBarIcons(lightBackground = !darkTheme)
    MaterialTheme(colorScheme = base.withCustomColors(background, accent, darkTheme), content = content)
}

private const val LIGHT_THRESHOLD = 0.35f

/** Texto/ícone legível sobre esta cor (claro sobre escuro, escuro sobre claro). */
internal fun Color.readableOn(): Color =
    if (luminance() > LIGHT_THRESHOLD) Color(0xFF1C1B1F) else Color(0xFFF4EFF4)

private fun ColorScheme.withCustomColors(background: Color?, accent: Color?, dark: Boolean): ColorScheme {
    var scheme = this

    if (background != null) {
        val on = background.readableOn()
        // Camadas de superfície: o fundo "puxado" um pouco para a cor do texto.
        fun layer(fraction: Float) = lerp(background, on, fraction)
        scheme = scheme.copy(
            background = background,
            onBackground = on,
            surface = background,
            onSurface = on,
            surfaceVariant = layer(0.12f),
            onSurfaceVariant = layer(0.72f),
            surfaceContainerLowest = layer(0.02f),
            surfaceContainerLow = layer(0.05f),
            surfaceContainer = layer(0.08f),
            surfaceContainerHigh = layer(0.11f),
            surfaceContainerHighest = layer(0.15f),
            outline = layer(0.5f),
            outlineVariant = layer(0.2f),
            inverseSurface = on,
            inverseOnSurface = background
        )
    }

    if (accent != null) {
        // No escuro o destaque clareia (como o tom 80 do M3) para manter contraste com o fundo.
        val primary = if (dark) lerp(accent, Color.White, 0.4f) else accent
        val container = lerp(scheme.surface, accent, if (dark) 0.4f else 0.22f)
        scheme = scheme.copy(
            primary = primary,
            onPrimary = primary.readableOn(),
            primaryContainer = container,
            onPrimaryContainer = container.readableOn(),
            secondary = primary,
            onSecondary = primary.readableOn(),
            secondaryContainer = container,
            onSecondaryContainer = container.readableOn(),
            surfaceTint = primary
        )
    }
    return scheme
}

/** Ícones da barra de status/navegação acompanham o fundo escolhido (evita ícone escuro sobre escuro). */
@Composable
private fun SystemBarIcons(lightBackground: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = lightBackground
        controller.isAppearanceLightNavigationBars = lightBackground
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
