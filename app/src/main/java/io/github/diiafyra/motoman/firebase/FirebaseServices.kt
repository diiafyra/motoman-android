package io.github.diiafyra.motoman.firebase

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import io.github.diiafyra.motoman.core.domain.analytics.AnalyticsEvent
import io.github.diiafyra.motoman.core.domain.analytics.AnalyticsTracker
import io.github.diiafyra.motoman.core.domain.maintenance.DueSoonThreshold
import io.github.diiafyra.motoman.core.domain.repository.FeatureFlags
import io.github.diiafyra.motoman.core.domain.repository.MaintenanceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Only created when the build has a `google-services.json`: without a Firebase app these calls would throw. */
object FirebaseSetup {
    /**
     * Debug builds report nothing, so development does not pollute the crash and usage numbers. Release
     * builds report crashes and the four events, with no personal data.
     */
    fun configure(
        context: Context,
        enabled: Boolean,
    ) {
        Firebase.crashlytics.isCrashlyticsCollectionEnabled = enabled
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enabled)
    }
}

class FirebaseAnalyticsTracker(
    private val analytics: FirebaseAnalytics,
) : AnalyticsTracker {
    // No parameters, on purpose: the event name is all the app reports.
    override fun track(event: AnalyticsEvent) = analytics.logEvent(event.eventName, null)

    companion object {
        fun create(context: Context) = FirebaseAnalyticsTracker(FirebaseAnalytics.getInstance(context))
    }
}

/**
 * Remote Config values with built-in defaults. The defaults apply until the first fetch finishes and
 * whenever a fetch fails, so the app never waits for the network to know its thresholds.
 */
class RemoteConfigServices(
    private val config: FirebaseRemoteConfig,
    minimumFetchIntervalSeconds: Long,
) : FeatureFlags,
    MaintenanceSettings,
    RemoteConfigRefresher {
    private data class Values(
        val nearbyEnabled: Boolean,
        val dueSoon: DueSoonThreshold,
    )

    private val values = MutableStateFlow(Values(DefaultServices.DEFAULT_NEARBY_ENABLED, DueSoonThreshold.DEFAULT))

    init {
        config.setConfigSettingsAsync(
            remoteConfigSettings {
                this.minimumFetchIntervalInSeconds =
                    minimumFetchIntervalSeconds
            },
        )
        config
            .setDefaultsAsync(
                mapOf(
                    KEY_NEARBY_ENABLED to DefaultServices.DEFAULT_NEARBY_ENABLED,
                    KEY_DUE_SOON_KM to DueSoonThreshold.DEFAULT.km.toLong(),
                    KEY_DUE_SOON_DAYS to DueSoonThreshold.DEFAULT.days.toLong(),
                ),
            ).addOnCompleteListener { values.value = read() }
    }

    override fun observeNearbyEnabled(): Flow<Boolean> = values.map { it.nearbyEnabled }

    override fun observeDueSoonThreshold(): Flow<DueSoonThreshold> = values.map { it.dueSoon }

    override fun refresh() {
        config.fetchAndActivate().addOnCompleteListener { values.value = read() }
    }

    private fun read() =
        Values(
            nearbyEnabled = config.getBoolean(KEY_NEARBY_ENABLED),
            dueSoon = sanitize(config.getLong(KEY_DUE_SOON_KM), config.getLong(KEY_DUE_SOON_DAYS)),
        )

    companion object {
        const val KEY_NEARBY_ENABLED = "nearby_enabled"
        const val KEY_DUE_SOON_KM = "due_soon_km"
        const val KEY_DUE_SOON_DAYS = "due_soon_days"

        /** A typo in the console must not break the app: out-of-range values fall back to the default. */
        internal fun sanitize(
            km: Long,
            days: Long,
        ): DueSoonThreshold =
            DueSoonThreshold(
                km = if (km in 0..MAX_KM) km.toInt() else DueSoonThreshold.DEFAULT.km,
                days = if (days in 0..MAX_DAYS) days.toInt() else DueSoonThreshold.DEFAULT.days,
            )

        private const val MAX_KM = 10_000L
        private const val MAX_DAYS = 365L

        fun create(minimumFetchIntervalSeconds: Long) =
            RemoteConfigServices(Firebase.remoteConfig, minimumFetchIntervalSeconds)
    }
}
