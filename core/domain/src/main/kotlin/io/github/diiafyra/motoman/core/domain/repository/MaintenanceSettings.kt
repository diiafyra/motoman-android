package io.github.diiafyra.motoman.core.domain.repository

import io.github.diiafyra.motoman.core.domain.maintenance.DueSoonThreshold
import kotlinx.coroutines.flow.Flow

/** The "due soon" distance and days, which can be tuned remotely without an app update. */
public interface MaintenanceSettings {
    public fun observeDueSoonThreshold(): Flow<DueSoonThreshold>
}
