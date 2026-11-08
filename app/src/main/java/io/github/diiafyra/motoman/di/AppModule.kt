package io.github.diiafyra.motoman.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.diiafyra.motoman.BuildConfig
import io.github.diiafyra.motoman.R
import io.github.diiafyra.motoman.core.domain.analytics.AnalyticsTracker
import io.github.diiafyra.motoman.core.domain.maintenance.MaintenanceTemplate
import io.github.diiafyra.motoman.core.domain.maintenance.TemplateNames
import io.github.diiafyra.motoman.core.domain.places.PlacesCache
import io.github.diiafyra.motoman.core.domain.repository.FeatureFlags
import io.github.diiafyra.motoman.core.domain.repository.MaintenanceSettings
import io.github.diiafyra.motoman.feature.nearby.FusedLocationProvider
import io.github.diiafyra.motoman.feature.nearby.LocationProvider
import io.github.diiafyra.motoman.feature.settings.AppCompatLanguageController
import io.github.diiafyra.motoman.feature.settings.LanguageController
import io.github.diiafyra.motoman.firebase.DefaultServices
import io.github.diiafyra.motoman.firebase.FirebaseAnalyticsTracker
import io.github.diiafyra.motoman.firebase.RemoteConfigRefresher
import io.github.diiafyra.motoman.firebase.RemoteConfigServices
import io.github.diiafyra.motoman.work.AndroidReminderNotifier
import io.github.diiafyra.motoman.work.ReminderNotifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /** Firebase-backed services in builds that have a Firebase project, no-op ones otherwise. */
    @Provides
    @Singleton
    fun services(
        @ApplicationContext context: Context,
    ): AppServices =
        if (BuildConfig.FIREBASE_ENABLED) {
            // Debug builds fetch on every start so a console change shows up at once; release builds every 12 hours.
            val remote = RemoteConfigServices.create(if (BuildConfig.DEBUG) 0 else REMOTE_CONFIG_INTERVAL_SECONDS)
            AppServices(FirebaseAnalyticsTracker.create(context), remote, remote, remote)
        } else {
            val default = DefaultServices()
            AppServices(default, default, default, default)
        }

    @Provides
    fun analyticsTracker(services: AppServices): AnalyticsTracker = services.analytics

    @Provides
    fun featureFlags(services: AppServices): FeatureFlags = services.flags

    @Provides
    fun maintenanceSettings(services: AppServices): MaintenanceSettings = services.settings

    @Provides
    fun remoteConfigRefresher(services: AppServices): RemoteConfigRefresher = services.refresher

    /** One cache for the whole process, so the Nearby screen reuses results across visits. */
    @Provides
    @Singleton
    fun placesCache(): PlacesCache = PlacesCache()

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

/** The four services that depend on whether Firebase is configured, chosen once. */
class AppServices(
    val analytics: AnalyticsTracker,
    val flags: FeatureFlags,
    val settings: MaintenanceSettings,
    val refresher: RemoteConfigRefresher,
)

private const val REMOTE_CONFIG_INTERVAL_SECONDS = 12L * 60 * 60

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindings {
    @Binds
    abstract fun reminderNotifier(impl: AndroidReminderNotifier): ReminderNotifier

    @Binds
    abstract fun languageController(impl: AppCompatLanguageController): LanguageController

    @Binds
    abstract fun locationProvider(impl: FusedLocationProvider): LocationProvider
}
