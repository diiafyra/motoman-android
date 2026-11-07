package io.github.diiafyra.motoman.feature.settings

import app.cash.turbine.test
import io.github.diiafyra.motoman.testing.FakePreferencesRepository
import io.github.diiafyra.motoman.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val preferences = FakePreferencesRepository()
    private val languages = FakeLanguages(AppLanguage.ENGLISH)
    private val viewModel by lazy { SettingsViewModel(preferences, languages) }

    @Test
    fun `shows the current language and reminder setting`() =
        runTest {
            viewModel.uiState.test {
                assertEquals(SettingsUiState(AppLanguage.ENGLISH, remindersEnabled = true), expectMostRecentItem())
            }
        }

    @Test
    fun `choosing a language applies it`() =
        runTest {
            viewModel.uiState.test {
                viewModel.onLanguageChange(AppLanguage.VIETNAMESE)

                assertEquals(AppLanguage.VIETNAMESE, languages.applied)
                assertEquals(AppLanguage.VIETNAMESE, expectMostRecentItem().language)
            }
        }

    @Test
    fun `turning reminders off is stored`() =
        runTest {
            viewModel.uiState.test {
                viewModel.onRemindersChange(false)

                assertFalse(preferences.remindersEnabled.value)
                assertFalse(expectMostRecentItem().remindersEnabled)
            }
        }

    private class FakeLanguages(
        var applied: AppLanguage,
    ) : LanguageController {
        override fun current() = applied

        override fun set(language: AppLanguage) {
            applied = language
        }
    }
}
