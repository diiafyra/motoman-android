package io.github.diiafyra.motoman.feature.nearby

import io.github.diiafyra.motoman.core.domain.repository.FeatureFlags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Everything on. Replaced by the Remote Config implementation when Firebase is added. */
class DefaultFeatureFlags
    @Inject
    constructor() : FeatureFlags {
        override fun observeNearbyEnabled(): Flow<Boolean> = flowOf(true)
    }
