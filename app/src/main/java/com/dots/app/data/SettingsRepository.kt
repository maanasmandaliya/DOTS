package com.dots.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dots.app.domain.model.HistoryEntry
import com.dots.app.domain.model.Preset
import com.dots.app.domain.model.WallpaperConfig
import com.dots.app.domain.model.WallpaperTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dots_settings")

enum class ThemePreference { DARK, LIGHT, SYSTEM }

data class AppSettings(
    /** The design being edited (always saved, even if not applied yet). */
    val config: WallpaperConfig = WallpaperConfig(),
    /** The design currently on the device; daily updates re-render this one. */
    val appliedConfig: WallpaperConfig? = null,
    val appliedTarget: WallpaperTarget = WallpaperTarget.BOTH,
    /** CountdownProgress.renderKey of the last applied render; lets the worker skip no-op updates. */
    val appliedRenderKey: String? = null,
    val target: WallpaperTarget = WallpaperTarget.BOTH,
    val autoUpdate: Boolean = true,
    val theme: ThemePreference = ThemePreference.DARK,
    val presets: List<Preset> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val customColors: List<Int> = emptyList(),
)

class SettingsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object Keys {
        val CONFIG = stringPreferencesKey("config")
        val APPLIED_CONFIG = stringPreferencesKey("applied_config")
        val APPLIED_TARGET = stringPreferencesKey("applied_target")
        val APPLIED_KEY = stringPreferencesKey("applied_render_key")
        val TARGET = stringPreferencesKey("target")
        val AUTO_UPDATE = booleanPreferencesKey("auto_update")
        val THEME = stringPreferencesKey("theme")
        val PRESETS = stringPreferencesKey("presets")
        val HISTORY = stringPreferencesKey("history")
        val CUSTOM_COLORS = stringPreferencesKey("custom_colors")
    }

    private val presetList = ListSerializer(Preset.serializer())
    private val historyList = ListSerializer(HistoryEntry.serializer())
    private val colorList = ListSerializer(Int.serializer())

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            config = decode(p[Keys.CONFIG], WallpaperConfig.serializer()) ?: WallpaperConfig(),
            appliedConfig = decode(p[Keys.APPLIED_CONFIG], WallpaperConfig.serializer()),
            appliedTarget = enumOr(p[Keys.APPLIED_TARGET], WallpaperTarget.BOTH),
            appliedRenderKey = p[Keys.APPLIED_KEY],
            target = enumOr(p[Keys.TARGET], WallpaperTarget.BOTH),
            autoUpdate = p[Keys.AUTO_UPDATE] ?: true,
            theme = enumOr(p[Keys.THEME], ThemePreference.DARK),
            presets = decode(p[Keys.PRESETS], presetList) ?: emptyList(),
            history = decode(p[Keys.HISTORY], historyList) ?: emptyList(),
            customColors = decode(p[Keys.CUSTOM_COLORS], colorList) ?: emptyList(),
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun saveConfig(config: WallpaperConfig) = context.dataStore.edit {
        it[Keys.CONFIG] = json.encodeToString(WallpaperConfig.serializer(), config)
    }

    suspend fun markApplied(config: WallpaperConfig, target: WallpaperTarget, renderKey: String) = context.dataStore.edit {
        it[Keys.APPLIED_CONFIG] = json.encodeToString(WallpaperConfig.serializer(), config)
        it[Keys.APPLIED_TARGET] = target.name
        it[Keys.APPLIED_KEY] = renderKey
    }

    suspend fun setTarget(target: WallpaperTarget) = context.dataStore.edit { it[Keys.TARGET] = target.name }

    suspend fun setAutoUpdate(enabled: Boolean) = context.dataStore.edit { it[Keys.AUTO_UPDATE] = enabled }

    suspend fun setTheme(theme: ThemePreference) = context.dataStore.edit { it[Keys.THEME] = theme.name }

    suspend fun savePresets(presets: List<Preset>) = context.dataStore.edit {
        it[Keys.PRESETS] = json.encodeToString(presetList, presets)
    }

    suspend fun saveHistory(history: List<HistoryEntry>) = context.dataStore.edit {
        it[Keys.HISTORY] = json.encodeToString(historyList, history)
    }

    suspend fun saveCustomColors(colors: List<Int>) = context.dataStore.edit {
        it[Keys.CUSTOM_COLORS] = json.encodeToString(colorList, colors)
    }

    private fun <T> decode(raw: String?, serializer: KSerializer<T>): T? = try {
        raw?.let { json.decodeFromString(serializer, it) }
    } catch (_: Exception) {
        // A corrupted or outdated value must never crash the app; fall back to defaults.
        null
    }

    private inline fun <reified E : Enum<E>> enumOr(raw: String?, default: E): E =
        raw?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default
}
