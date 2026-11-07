package io.github.diiafyra.motoman.feature.nearby

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import io.github.diiafyra.motoman.BuildConfig
import io.github.diiafyra.motoman.R
import io.github.diiafyra.motoman.core.domain.model.GeoPoint
import io.github.diiafyra.motoman.core.domain.model.Place
import io.github.diiafyra.motoman.core.domain.model.PlaceKind
import io.github.diiafyra.motoman.core.domain.result.DataError
import io.github.diiafyra.motoman.ui.designsystem.component.EmptyState
import io.github.diiafyra.motoman.ui.designsystem.component.LoadingState
import io.github.diiafyra.motoman.ui.format.formatDecimal
import io.github.diiafyra.motoman.ui.format.formatInteger

@Composable
fun NearbyScreen(viewModel: NearbyViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result.values.any { it }
            // After a refusal, Android stops showing the dialog once no rationale is offered.
            val canAskAgain = context.findActivity()?.let(::canAskAgain) ?: true
            viewModel.onPermissionResult(granted, canAskAgain)
        }

    // Re-checked each time the screen comes back: the user may have changed it in system settings.
    LifecycleResumeEffect(Unit) {
        if (hasLocationPermission(context)) {
            if (state is NearbyUiState.NeedsPermission) viewModel.load()
        }
        onPauseOrDispose {}
    }

    NearbyContent(
        state = state,
        onAllow = {
            launcher.launch(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            )
        },
        onOpenSettings = {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ),
            )
        },
        onRetry = viewModel::load,
        onDirections = { place -> context.startActivity(directionsIntent(place)) },
    )
}

@Composable
internal fun NearbyContent(
    state: NearbyUiState,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onDirections: (Place) -> Unit,
) {
    when (state) {
        NearbyUiState.Disabled -> {
            EmptyState(
                title = stringResource(R.string.nearby_disabled_title),
                body = stringResource(R.string.nearby_disabled_body),
                icon = ImageVector.vectorResource(R.drawable.ic_nearby),
            )
        }

        is NearbyUiState.NeedsPermission -> {
            EmptyState(
                title = stringResource(R.string.nearby_permission_title),
                body =
                    stringResource(
                        if (state.canAsk) R.string.nearby_permission_body else R.string.nearby_permission_settings_body,
                    ),
                icon = ImageVector.vectorResource(R.drawable.ic_nearby),
                actionLabel =
                    stringResource(
                        if (state.canAsk) R.string.nearby_permission_allow else R.string.nearby_open_settings,
                    ),
                onAction = if (state.canAsk) onAllow else onOpenSettings,
            )
        }

        NearbyUiState.Locating -> {
            Progress(stringResource(R.string.nearby_locating))
        }

        NearbyUiState.Loading -> {
            Progress(stringResource(R.string.nearby_loading))
        }

        NearbyUiState.LocationUnavailable -> {
            EmptyState(
                title = stringResource(R.string.nearby_location_unavailable_title),
                body = stringResource(R.string.nearby_location_unavailable_body),
                icon = ImageVector.vectorResource(R.drawable.ic_nearby),
                actionLabel = stringResource(R.string.nearby_retry),
                onAction = onRetry,
            )
        }

        is NearbyUiState.Empty -> {
            EmptyState(
                title = stringResource(R.string.nearby_empty_title),
                body = stringResource(R.string.nearby_empty_body),
                icon = ImageVector.vectorResource(R.drawable.ic_nearby),
                actionLabel = stringResource(R.string.nearby_retry),
                onAction = onRetry,
            )
        }

        is NearbyUiState.Error -> {
            EmptyState(
                title = stringResource(R.string.nearby_title),
                body = stringResource(errorMessage(state.error)),
                icon = ImageVector.vectorResource(R.drawable.ic_nearby),
                actionLabel = stringResource(R.string.nearby_retry),
                onAction = onRetry,
            )
        }

        is NearbyUiState.Content -> {
            Places(state, onDirections)
        }
    }
}

private fun errorMessage(error: DataError): Int =
    when (error) {
        DataError.NoConnection -> R.string.nearby_error_no_connection
        DataError.Timeout -> R.string.nearby_error_timeout
        is DataError.Server -> R.string.nearby_error_server
        DataError.InvalidData -> R.string.nearby_error_invalid
    }

@Composable
private fun Progress(text: String) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        LoadingState(Modifier.height(96.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun Places(
    state: NearbyUiState.Content,
    onDirections: (Place) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (BuildConfig.HAS_MAPS_KEY) {
            PlacesMap(state, Modifier.fillMaxWidth().height(260.dp))
        } else {
            Text(
                stringResource(R.string.nearby_no_map),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(state.places, key = { it.place.id }) { item -> PlaceRow(item, onDirections) }
        }
    }
}

@Composable
private fun PlacesMap(
    state: NearbyUiState.Content,
    modifier: Modifier,
) {
    val center = state.center.toLatLng()
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(center, MAP_ZOOM) }
    LaunchedEffect(center) { camera.position = CameraPosition.fromLatLngZoom(center, MAP_ZOOM) }
    val mapDescription = stringResource(R.string.nearby_title)
    GoogleMap(
        modifier = modifier.semantics { contentDescription = mapDescription },
        cameraPositionState = camera,
        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
    ) {
        Marker(state = remember(center) { MarkerState(center) })
        state.places.forEach { item ->
            Marker(
                state = remember(item.place.id) { MarkerState(item.place.location.toLatLng()) },
                title = placeTitle(item.place),
            )
        }
    }
}

@Composable
private fun PlaceRow(
    item: PlaceWithDistance,
    onDirections: (Place) -> Unit,
) {
    val title = placeTitle(item.place)
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                listOfNotNull(formatDistance(item.distanceMeters), item.place.openingHours).joinToString(" · "),
            )
        },
        trailingContent = {
            val description = stringResource(R.string.nearby_directions_named, title)
            OutlinedButton(
                onClick = { onDirections(item.place) },
                modifier = Modifier.semantics { contentDescription = description },
            ) { Text(stringResource(R.string.nearby_directions)) }
        },
    )
}

@Composable
private fun placeTitle(place: Place): String =
    place.name
        ?: stringResource(
            when (place.kind) {
                PlaceKind.FUEL_STATION -> R.string.nearby_unnamed_fuel
                PlaceKind.CAR_REPAIR -> R.string.nearby_unnamed_car_repair
                PlaceKind.MOTORBIKE_REPAIR -> R.string.nearby_unnamed_motorbike_repair
            },
        )

@Composable
private fun formatDistance(meters: Int): String =
    if (meters < 1_000) {
        stringResource(R.string.nearby_distance_m, formatInteger(meters.toLong()))
    } else {
        stringResource(R.string.nearby_distance_km, formatDecimal(meters / 1_000.0, 1))
    }

private fun GeoPoint.toLatLng() = LatLng(latitude, longitude)

private const val MAP_ZOOM = 14f

private fun hasLocationPermission(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION).any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/**
 * After a refusal, Android offers a rationale while it will still show the dialog. Once it does not,
 * the user has refused for good and only the app settings can turn location on.
 */
private fun canAskAgain(activity: Activity): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun directionsIntent(place: Place): Intent {
    val label = Uri.encode(place.name ?: "")
    val lat = place.location.latitude
    val lon = place.location.longitude
    return Intent(Intent.ACTION_VIEW, "geo:$lat,$lon?q=$lat,$lon($label)".toUri())
}
