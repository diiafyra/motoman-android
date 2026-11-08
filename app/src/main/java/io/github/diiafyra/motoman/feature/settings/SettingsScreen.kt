package io.github.diiafyra.motoman.feature.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.diiafyra.motoman.BuildConfig
import io.github.diiafyra.motoman.R

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(state, onBack, viewModel::onLanguageChange, viewModel::onRemindersChange)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onRemindersChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val language = LocalConfiguration.current.locales[0].language
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SectionTitle(stringResource(R.string.settings_language))
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { language ->
                    val selected = state.language == language
                    ListItem(
                        headlineContent = { Text(languageName(language)) },
                        leadingContent = { RadioButton(selected = selected, onClick = null) },
                        modifier =
                            Modifier.selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { if (!selected) onLanguageChange(language) },
                            ),
                    )
                }
            }
            HorizontalDivider()

            SectionTitle(stringResource(R.string.settings_reminders_section))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_reminders)) },
                supportingContent = { Text(stringResource(R.string.settings_reminders_body)) },
                trailingContent = { Switch(checked = state.remindersEnabled, onCheckedChange = null) },
                modifier =
                    Modifier.toggleable(
                        value = state.remindersEnabled,
                        role = Role.Switch,
                        onValueChange = onRemindersChange,
                    ),
            )
            HorizontalDivider()

            SectionTitle(stringResource(R.string.settings_about))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
                supportingContent = { Text(BuildConfig.VERSION_NAME) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_privacy)) },
                modifier =
                    Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, privacyPolicyUrl(language).toUri()))
                    },
            )
        }
    }
}

/** Each language is named in itself, so a user who cannot read the current one still finds theirs. */
@Composable
private fun languageName(language: AppLanguage): String =
    when (language) {
        AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
        AppLanguage.VIETNAMESE -> stringResource(R.string.language_vietnamese)
        AppLanguage.ENGLISH -> stringResource(R.string.language_english)
    }

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp).semantics { heading() },
    )
}

private const val PRIVACY_POLICY_BASE_URL = "https://diiafyra.github.io/motoman-android"

/** The policy in the language the app is shown in: Vietnamese for "vi", English for anything else. */
internal fun privacyPolicyUrl(languageTag: String): String =
    if (languageTag == "vi") "$PRIVACY_POLICY_BASE_URL/privacy-vi/" else "$PRIVACY_POLICY_BASE_URL/privacy/"
