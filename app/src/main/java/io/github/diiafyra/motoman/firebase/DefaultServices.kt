package io.github.diiafyra.motoman.firebase

import io.github.diiafyra.motoman.core.domain.analytics.AnalyticsEvent
import io.github.diiafyra.motoman.core.domain.analytics.AnalyticsTracker
import io.github.diiafyra.motoman.core.domain.maintenance.DueSoonThreshold
import io.github.diiafyra.motoman.core.domain.repository.FeatureFlags
import io.github.diiafyra.motoman.core.domain.repository.MaintenanceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Starts a refresh of the remote values. Does nothing when Firebase is not configured. */
interface RemoteConfigRefresher {
    fun refresh()
}

/**
 * What the app uses when there is no Firebase project: no events are sent, and the settings keep their
 * built-in values. Contributors and CI build and run the app with this.
 */
class DefaultServices :
    AnalyticsTracker,
    FeatureFlags,
    MaintenanceSettings,
    RemoteConfigRefresher {
    override fun track(event: AnalyticsEvent) = Unit

    override fun observeNearbyEnabled(): Flow<Boolean> = flowOf(DEFAULT_NEARBY_ENABLED)

    override fun observeDueSoonThreshold(): Flow<DueSoonThreshold> = flowOf(DueSoonThreshold.DEFAULT)

    override fun refresh() = Unit

    companion object {
        const val DEFAULT_NEARBY_ENABLED = true
    }
}
