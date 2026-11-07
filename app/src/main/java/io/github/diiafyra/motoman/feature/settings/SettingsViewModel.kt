package io.github.diiafyra.motoman.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diiafyra.motoman.core.domain.repository.PreferencesRepository
import io.github.diiafyra.motoman.feature.vehicle.STOP_TIMEOUT_MS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val remindersEnabled: Boolean = true,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val preferences: PreferencesRepository,
        private val languages: LanguageController,
    ) : ViewModel() {
        private val language = MutableStateFlow(languages.current())

        val uiState: StateFlow<SettingsUiState> =
            combine(
                language,
                preferences.observeRemindersEnabled(),
            ) { lang, reminders -> SettingsUiState(lang, reminders) }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    SettingsUiState(language.value),
                )

        /** The activity is recreated in the new language right after this. */
        fun onLanguageChange(language: AppLanguage) {
            this.language.value = language
            languages.set(language)
        }

        fun onRemindersChange(enabled: Boolean) {
            viewModelScope.launch { preferences.setRemindersEnabled(enabled) }
        }
    }
