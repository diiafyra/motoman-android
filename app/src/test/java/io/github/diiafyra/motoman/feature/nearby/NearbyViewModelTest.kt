package io.github.diiafyra.motoman.feature.nearby

import io.github.diiafyra.motoman.core.domain.model.GeoPoint
import io.github.diiafyra.motoman.core.domain.model.Place
import io.github.diiafyra.motoman.core.domain.model.PlaceKind
import io.github.diiafyra.motoman.core.domain.places.FindNearbyPlaces
import io.github.diiafyra.motoman.core.domain.places.PlacesCache
import io.github.diiafyra.motoman.core.domain.repository.FeatureFlags
import io.github.diiafyra.motoman.core.domain.repository.PlacesRepository
import io.github.diiafyra.motoman.core.domain.result.AppResult
import io.github.diiafyra.motoman.core.domain.result.DataError
import io.github.diiafyra.motoman.testing.FakeClock
import io.github.diiafyra.motoman.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NearbyViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val here = GeoPoint(21.0285, 105.8542)
    private val near = Place("node/1", PlaceKind.FUEL_STATION, GeoPoint(21.0290, 105.8542), "Near", null, null)
    private val far = Place("node/2", PlaceKind.CAR_REPAIR, GeoPoint(21.0400, 105.8542), null, null, "24/7")

    private val places = FakePlaces(AppResult.Success(listOf(near, far)))
    private val location = FakeLocation(here)
    private val flags = FakeFlags()

    private fun viewModel() = NearbyViewModel(FindNearbyPlaces(places, PlacesCache(), FakeClock()), location, flags)

    @Test
    fun `starts by asking for permission`() {
        assertEquals(NearbyUiState.NeedsPermission(canAsk = true), viewModel().uiState.value)
    }

    @Test
    fun `a refusal keeps asking possible only while Android still shows the dialog`() {
        val viewModel = viewModel()

        viewModel.onPermissionResult(granted = false, canAskAgain = true)
        assertEquals(NearbyUiState.NeedsPermission(canAsk = true), viewModel.uiState.value)

        viewModel.onPermissionResult(granted = false, canAskAgain = false)
        assertEquals(NearbyUiState.NeedsPermission(canAsk = false), viewModel.uiState.value)
        assertEquals(0, places.calls)
    }

    @Test
    fun `once permission is granted the places show nearest first with their distance`() {
        val viewModel = viewModel()

        viewModel.onPermissionResult(granted = true, canAskAgain = true)

        val state = viewModel.uiState.value as NearbyUiState.Content
        assertEquals(here, state.center)
        assertEquals(listOf("node/1", "node/2"), state.places.map { it.place.id })
        // 0.0005° of latitude is about 56 m; 0.0115° about 1,279 m.
        assertEquals(56.0, state.places[0].distanceMeters.toDouble(), 1.0)
        assertEquals(1_279.0, state.places[1].distanceMeters.toDouble(), 5.0)
    }

    @Test
    fun `no position means location is unavailable and no request is made`() {
        location.point = null
        val viewModel = viewModel()

        viewModel.load()

        assertEquals(NearbyUiState.LocationUnavailable, viewModel.uiState.value)
        assertEquals(0, places.calls)
    }

    @Test
    fun `an empty answer is its own state`() {
        places.result = AppResult.Success(emptyList())
        val viewModel = viewModel()

        viewModel.load()

        assertEquals(NearbyUiState.Empty(here), viewModel.uiState.value)
    }

    @Test
    fun `each failure is kept as its error and retrying can succeed`() {
        val viewModel = viewModel()
        for (error in listOf(DataError.NoConnection, DataError.Timeout, DataError.Server(503), DataError.InvalidData)) {
            places.result = AppResult.Failure(error)
            viewModel.load()
            assertEquals(NearbyUiState.Error(error), viewModel.uiState.value)
        }

        places.result = AppResult.Success(listOf(near))
        viewModel.load()

        assertTrue(viewModel.uiState.value is NearbyUiState.Content)
    }

    @Test
    fun `switched off remotely shows the paused state without locating`() {
        flags.nearbyEnabled.value = false
        val viewModel = viewModel()

        viewModel.load()

        assertEquals(NearbyUiState.Disabled, viewModel.uiState.value)
        assertEquals(0, location.calls)
    }

    @Test
    fun `while locating or loading the screen shows progress`() {
        val gate = CompletableDeferred<GeoPoint?>()
        location.gate = gate
        val viewModel = viewModel()

        viewModel.load()
        assertEquals(NearbyUiState.Locating, viewModel.uiState.value)

        gate.complete(here)
        assertTrue(viewModel.uiState.value is NearbyUiState.Content)
    }

    private class FakePlaces(
        var result: AppResult<List<Place>>,
    ) : PlacesRepository {
        var calls = 0

        override suspend fun nearby(
            center: GeoPoint,
            radiusMeters: Int,
        ): AppResult<List<Place>> {
            calls++
            return result
        }
    }

    private class FakeLocation(
        var point: GeoPoint?,
    ) : LocationProvider {
        var calls = 0
        var gate: CompletableDeferred<GeoPoint?>? = null

        override suspend fun current(): GeoPoint? {
            calls++
            return gate?.await() ?: point
        }
    }

    private class FakeFlags : FeatureFlags {
        val nearbyEnabled = MutableStateFlow(true)

        override fun observeNearbyEnabled(): Flow<Boolean> = nearbyEnabled
    }
}
