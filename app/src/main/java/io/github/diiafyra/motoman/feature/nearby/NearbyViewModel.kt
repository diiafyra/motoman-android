package io.github.diiafyra.motoman.feature.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diiafyra.motoman.core.domain.model.GeoPoint
import io.github.diiafyra.motoman.core.domain.model.Place
import io.github.diiafyra.motoman.core.domain.model.distanceMeters
import io.github.diiafyra.motoman.core.domain.places.FindNearbyPlaces
import io.github.diiafyra.motoman.core.domain.repository.FeatureFlags
import io.github.diiafyra.motoman.core.domain.result.AppResult
import io.github.diiafyra.motoman.core.domain.result.DataError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaceWithDistance(
    val place: Place,
    val distanceMeters: Int,
)

/** One state per thing the user can be looking at. The screen draws each of them. */
sealed interface NearbyUiState {
    /** Switched off remotely, for example while the data service has problems. */
    data object Disabled : NearbyUiState

    /** Permission not granted yet (or refused). [canAsk] is false once Android will not show the dialog again. */
    data class NeedsPermission(
        val canAsk: Boolean = true,
    ) : NearbyUiState

    data object Locating : NearbyUiState

    /** Permission granted but no position: location services are off, or there is no fix. */
    data object LocationUnavailable : NearbyUiState

    data object Loading : NearbyUiState

    data class Content(
        val center: GeoPoint,
        val places: List<PlaceWithDistance>,
    ) : NearbyUiState

    /** The request worked and nothing is within range. */
    data class Empty(
        val center: GeoPoint,
    ) : NearbyUiState

    data class Error(
        val error: DataError,
    ) : NearbyUiState
}

@HiltViewModel
class NearbyViewModel
    @Inject
    constructor(
        private val findNearby: FindNearbyPlaces,
        private val location: LocationProvider,
        private val flags: FeatureFlags,
    ) : ViewModel() {
        private val state = MutableStateFlow<NearbyUiState>(NearbyUiState.NeedsPermission())
        val uiState: StateFlow<NearbyUiState> = state.asStateFlow()

        private var job: Job? = null

        /** The screen reports what the system said, so the state matches the permission. */
        fun onPermissionResult(
            granted: Boolean,
            canAskAgain: Boolean,
        ) {
            if (granted) load() else state.value = NearbyUiState.NeedsPermission(canAsk = canAskAgain)
        }

        /** Looks up the position and the places around it. Safe to call again to retry. */
        fun load() {
            job?.cancel()
            job =
                viewModelScope.launch {
                    if (!flags.observeNearbyEnabled().first()) {
                        state.value = NearbyUiState.Disabled
                        return@launch
                    }
                    state.value = NearbyUiState.Locating
                    val center = location.current()
                    if (center == null) {
                        state.value = NearbyUiState.LocationUnavailable
                        return@launch
                    }
                    state.value = NearbyUiState.Loading
                    state.update {
                        when (val result = findNearby(center)) {
                            is AppResult.Failure -> {
                                NearbyUiState.Error(result.error)
                            }

                            is AppResult.Success -> {
                                if (result.value.isEmpty()) {
                                    NearbyUiState.Empty(center)
                                } else {
                                    NearbyUiState.Content(
                                        center,
                                        result.value.map {
                                            PlaceWithDistance(
                                                it,
                                                distanceMeters(center, it.location).toInt(),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }
