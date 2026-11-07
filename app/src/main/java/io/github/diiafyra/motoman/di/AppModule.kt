package io.github.diiafyra.motoman.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.diiafyra.motoman.R
import io.github.diiafyra.motoman.core.domain.maintenance.MaintenanceTemplate
import io.github.diiafyra.motoman.core.domain.maintenance.TemplateNames
import io.github.diiafyra.motoman.feature.settings.AppCompatLanguageController
import io.github.diiafyra.motoman.feature.settings.LanguageController
import io.github.diiafyra.motoman.work.AndroidReminderNotifier
import io.github.diiafyra.motoman.work.ReminderNotifier

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /** Template names in the app's current language, read when a vehicle is created. */
    @Provides
    fun templateNames(
        @ApplicationContext context: Context,
    ): TemplateNames =
        TemplateNames { template ->
            context.getString(
                when (template) {
                    MaintenanceTemplate.OIL_CHANGE -> R.string.template_oil_change
                    MaintenanceTemplate.AIR_FILTER -> R.string.template_air_filter
                    MaintenanceTemplate.TYRES -> R.string.template_tyres
                    MaintenanceTemplate.BRAKES -> R.string.template_brakes
                    MaintenanceTemplate.INSPECTION -> R.string.template_inspection
                },
            )
        }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindings {
    @Binds
    abstract fun reminderNotifier(impl: AndroidReminderNotifier): ReminderNotifier

    @Binds
    abstract fun languageController(impl: AppCompatLanguageController): LanguageController
}
