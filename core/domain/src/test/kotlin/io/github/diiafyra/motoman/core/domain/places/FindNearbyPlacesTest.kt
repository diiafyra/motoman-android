package io.github.diiafyra.motoman.core.domain.places

import io.github.diiafyra.motoman.core.domain.model.GeoPoint
import io.github.diiafyra.motoman.core.domain.model.Place
import io.github.diiafyra.motoman.core.domain.model.PlaceKind
import io.github.diiafyra.motoman.core.domain.repository.PlacesRepository
import io.github.diiafyra.motoman.core.domain.result.AppResult
import io.github.diiafyra.motoman.core.domain.result.DataError
import io.github.diiafyra.motoman.core.domain.util.Clock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FindNearbyPlacesTest {
    private val here = GeoPoint(21.0285, 105.8542)
    private val sameCell = GeoPoint(21.0299, 105.8599)
    private val farAway = GeoPoint(21.1, 105.9)
    private val place = Place("node/1", PlaceKind.FUEL_STATION, here, "Petrolimex", null, null)

    private val repository = FakeRepository()
    private val clock = FakeClock()
    private val find = FindNearbyPlaces(repository, PlacesCache(), clock)

    @Test
    fun `the second request in the same cell comes from the cache`() =
        runTest {
            find(here)
            val second = find(sameCell)

            assertEquals(AppResult.Success(listOf(place)), second)
            assertEquals(1, repository.calls)
        }

    @Test
    fun `another cell asks the server`() =
        runTest {
            find(here)
            find(farAway)

            assertEquals(2, repository.calls)
        }

    @Test
    fun `after half an hour the cell is fetched again`() =
        runTest {
            find(here)
            clock.nowMillis += 30 * 60 * 1_000

            find(here)

            assertEquals(2, repository.calls)
        }

    @Test
    fun `a failure is returned and not cached`() =
        runTest {
            repository.result = AppResult.Failure(DataError.Timeout)
            assertEquals(AppResult.Failure(DataError.Timeout), find(here))

            repository.result = AppResult.Success(listOf(place))
            assertEquals(AppResult.Success(listOf(place)), find(here))
            assertEquals(2, repository.calls)
        }

    private inner class FakeRepository : PlacesRepository {
        var calls = 0
        var result: AppResult<List<Place>> = AppResult.Success(listOf(place))

        override suspend fun nearby(
            center: GeoPoint,
            radiusMeters: Int,
        ): AppResult<List<Place>> {
            calls++
            return result
        }
    }

    private class FakeClock : Clock {
        var nowMillis = 1_000_000L

        override fun now(): Instant = Instant.ofEpochMilli(nowMillis)

        override fun today(): LocalDate = LocalDate.of(2026, 11, 8)

        override fun zone(): ZoneId = ZoneId.of("UTC")
    }
}
