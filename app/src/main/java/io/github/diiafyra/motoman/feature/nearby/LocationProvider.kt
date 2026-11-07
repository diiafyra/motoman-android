package io.github.diiafyra.motoman.feature.nearby

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.diiafyra.motoman.core.domain.model.GeoPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

/** The user's current position. Behind an interface so ViewModels test with a fake. */
interface LocationProvider {
    /** Null when the position cannot be found: location services off, or no fix. Needs permission. */
    suspend fun current(): GeoPoint?
}

class FusedLocationProvider
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : LocationProvider {
        @SuppressLint("MissingPermission") // The screen asks for permission before calling this.
        override suspend fun current(): GeoPoint? =
            suspendCancellableCoroutine { continuation ->
                val tokenSource = CancellationTokenSource()
                continuation.invokeOnCancellation { tokenSource.cancel() }
                LocationServices
                    .getFusedLocationProviderClient(context)
                    // Balanced accuracy is plenty for stations a few km away and uses less battery.
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token)
                    .addOnSuccessListener { location ->
                        continuation.resume(location?.let { GeoPoint(it.latitude, it.longitude) })
                    }.addOnFailureListener { continuation.resume(null) }
            }
    }
