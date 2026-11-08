package io.github.diiafyra.motoman.core.domain.analytics

/**
 * The only things the app reports, each when something happens for the first time in that way. An event
 * has a name and nothing else: no vehicle names, no odometer values, no locations, no ids.
 */
public enum class AnalyticsEvent(
    public val eventName: String,
) {
    VEHICLE_ADDED("vehicle_added"),
    FUEL_LOGGED("fuel_logged"),
    MAINTENANCE_DONE("maintenance_done"),
    NEARBY_OPENED("nearby_opened"),
}

public interface AnalyticsTracker {
    public fun track(event: AnalyticsEvent)
}
