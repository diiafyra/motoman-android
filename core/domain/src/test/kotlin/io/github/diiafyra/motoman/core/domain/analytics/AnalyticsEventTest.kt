package io.github.diiafyra.motoman.core.domain.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsEventTest {
    @Test
    fun `there are exactly four events, with the names the privacy policy lists`() {
        assertEquals(
            listOf("vehicle_added", "fuel_logged", "maintenance_done", "nearby_opened"),
            AnalyticsEvent.entries.map { it.eventName },
        )
    }
}
