package com.dangle.jobtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dangle.jobtracker.data.repository.UserPreferencesRepository
import com.dangle.jobtracker.domain.model.ThemeConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val themeConfig: StateFlow<ThemeConfig> = userPreferencesRepository.themeConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeConfig.FOLLOW_SYSTEM
        )

    fun setThemeConfig(config: ThemeConfig) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeConfig(config)
        }
    }
}
