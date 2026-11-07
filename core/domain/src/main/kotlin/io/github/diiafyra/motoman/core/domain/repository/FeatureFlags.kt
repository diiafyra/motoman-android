package io.github.diiafyra.motoman.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Switches that can be changed without an app update. The values start as safe defaults and are
 * replaced by Firebase Remote Config values once they are fetched.
 */
public interface FeatureFlags {
    /** Turns the Nearby screen off remotely if the Overpass service has problems. */
    public fun observeNearbyEnabled(): Flow<Boolean>
}
