package io.github.diiafyra.motoman.firebase

import io.github.diiafyra.motoman.core.domain.analytics.AnalyticsEvent
import io.github.diiafyra.motoman.core.domain.maintenance.DueSoonThreshold
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteConfigServicesTest {
    @Test
    fun `sensible remote values are used as they are`() {
        assertEquals(DueSoonThreshold(km = 800, days = 21), RemoteConfigServices.sanitize(800, 21))
        assertEquals(DueSoonThreshold(km = 0, days = 0), RemoteConfigServices.sanitize(0, 0))
    }

    @Test
    fun `a value typed wrongly in the console falls back to the default, each on its own`() {
        val default = DueSoonThreshold.DEFAULT

        assertEquals(DueSoonThreshold(km = default.km, days = 21), RemoteConfigServices.sanitize(-1, 21))
        assertEquals(DueSoonThreshold(km = 800, days = default.days), RemoteConfigServices.sanitize(800, 5_000))
        assertEquals(default, RemoteConfigServices.sanitize(2_000_000_000, -3))
    }

    @Test
    fun `without Firebase nothing is sent and the settings keep their defaults`() =
        runTest {
            val services = DefaultServices()

            services.track(AnalyticsEvent.FUEL_LOGGED)
            services.refresh()

            assertEquals(DueSoonThreshold.DEFAULT, services.observeDueSoonThreshold().first())
            assertEquals(true, services.observeNearbyEnabled().first())
        }
}
