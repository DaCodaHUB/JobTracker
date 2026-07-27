package com.dangle.jobtracker.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dangle.jobtracker.domain.model.ThemeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface UserPreferencesRepository {
    val themeConfig: Flow<ThemeConfig>
    suspend fun setThemeConfig(config: ThemeConfig)
}

class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val THEME_CONFIG = stringPreferencesKey("theme_config")
    }

    override val themeConfig: Flow<ThemeConfig> = dataStore.data.map { preferences ->
        val themeString = preferences[PreferencesKeys.THEME_CONFIG]
        ThemeConfig.valueOf(themeString ?: ThemeConfig.FOLLOW_SYSTEM.name)
    }

    override suspend fun setThemeConfig(config: ThemeConfig) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_CONFIG] = config.name
        }
    }
}
