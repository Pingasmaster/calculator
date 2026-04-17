package com.calculator.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "calculator_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class UserPreferences(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.dataStore)

    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
    private val OLED_BLACK_KEY = booleanPreferencesKey("oled_black")

    val themeMode: Flow<ThemeMode> =
        dataStore.data.map {
            when (it[THEME_MODE_KEY]) {
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }

    val dynamicColorEnabled: Flow<Boolean> =
        dataStore.data.map { it[DYNAMIC_COLOR_KEY] ?: true }

    val oledBlackEnabled: Flow<Boolean> =
        dataStore.data.map { it[OLED_BLACK_KEY] ?: false }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit {
            it[THEME_MODE_KEY] = when (mode) {
                ThemeMode.SYSTEM -> "system"
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
            }
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled }
    }

    suspend fun setOledBlack(enabled: Boolean) {
        dataStore.edit { it[OLED_BLACK_KEY] = enabled }
    }
}
