package io.github.diiafyra.motoman.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject

/** The app's own language, separate from the phone's. */
enum class AppLanguage(
    /** BCP 47 tag, or null to follow the system. */
    val tag: String?,
) {
    SYSTEM(null),
    VIETNAMESE("vi"),
    ENGLISH("en"),
}

interface LanguageController {
    fun current(): AppLanguage

    fun set(language: AppLanguage)
}

/**
 * Per-app language through AppCompat, which uses the system setting on Android 13+ and stores the
 * choice itself on older versions (see the `autoStoreLocales` service in the manifest).
 */
class AppCompatLanguageController
    @Inject
    constructor() : LanguageController {
        override fun current(): AppLanguage {
            val tag = AppCompatDelegate.getApplicationLocales()[0]?.language
            return AppLanguage.entries.firstOrNull { it.tag != null && it.tag == tag } ?: AppLanguage.SYSTEM
        }

        override fun set(language: AppLanguage) {
            val locales = language.tag?.let(LocaleListCompat::forLanguageTags) ?: LocaleListCompat.getEmptyLocaleList()
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
