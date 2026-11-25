package com.projet.app.ui.theme

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import com.projet.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Personalização de cores. `null` = usar o padrão do sistema (Dynamic Color / claro-escuro). */
data class ThemeSettings(
    val background: Int? = null, // ARGB
    val accent: Int? = null      // ARGB
)

/**
 * Guarda a cor de fundo e a de destaque. SharedPreferences basta aqui (2 inteiros, sem
 * dependência extra) e a leitura é síncrona, então o tema já nasce certo, sem "piscar".
 */
class ThemePrefs(context: Context) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<ThemeSettings> = _settings.asStateFlow()

    fun setBackground(argb: Int?) = save(_settings.value.copy(background = argb))
    fun setAccent(argb: Int?) = save(_settings.value.copy(accent = argb))

    private fun save(new: ThemeSettings) {
        _settings.value = new
        prefs.edit {
            if (new.background == null) remove(KEY_BACKGROUND) else putInt(KEY_BACKGROUND, new.background)
            if (new.accent == null) remove(KEY_ACCENT) else putInt(KEY_ACCENT, new.accent)
        }
    }

    private fun load() = ThemeSettings(
        background = prefs.takeIf { it.contains(KEY_BACKGROUND) }?.getInt(KEY_BACKGROUND, 0),
        accent = prefs.takeIf { it.contains(KEY_ACCENT) }?.getInt(KEY_ACCENT, 0)
    )

    private companion object {
        const val FILE = "projet_theme"
        const val KEY_BACKGROUND = "background"
        const val KEY_ACCENT = "accent"
    }
}

data class ColorOption(@StringRes val nameRes: Int, val color: Color)

/** Cores oferecidas no seletor (claras e escuras para o fundo; tons médios para o destaque). */
object ThemePalettes {
    val backgrounds = listOf(
        ColorOption(R.string.bg_white, Color(0xFFFFFFFF)),
        ColorOption(R.string.bg_cream, Color(0xFFFFF8E7)),
        ColorOption(R.string.bg_light_gray, Color(0xFFF1F3F4)),
        ColorOption(R.string.bg_ice, Color(0xFFEAF2FB)),
        ColorOption(R.string.bg_mint, Color(0xFFE8F5EE)),
        ColorOption(R.string.bg_rose, Color(0xFFFCEFF3)),
        ColorOption(R.string.bg_graphite, Color(0xFF1E1F22)),
        ColorOption(R.string.bg_black, Color(0xFF000000)),
        ColorOption(R.string.bg_night, Color(0xFF0F1B2D)),
        ColorOption(R.string.bg_forest, Color(0xFF10231A)),
        ColorOption(R.string.bg_wine, Color(0xFF2A1218)),
        ColorOption(R.string.bg_violet, Color(0xFF1D1630))
    )

    val accents = listOf(
        ColorOption(R.string.accent_purple, Color(0xFF6750A4)),
        ColorOption(R.string.accent_blue, Color(0xFF1E6FD9)),
        ColorOption(R.string.accent_green, Color(0xFF2E7D32)),
        ColorOption(R.string.accent_orange, Color(0xFFE65100)),
        ColorOption(R.string.accent_red, Color(0xFFC62828)),
        ColorOption(R.string.accent_pink, Color(0xFFD81B60)),
        ColorOption(R.string.accent_teal, Color(0xFF00796B)),
        ColorOption(R.string.accent_amber, Color(0xFFF9A825)),
        ColorOption(R.string.accent_brown, Color(0xFF6D4C41)),
        ColorOption(R.string.accent_slate, Color(0xFF455A64))
    )
}
