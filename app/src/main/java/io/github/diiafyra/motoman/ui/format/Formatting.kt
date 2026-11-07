package io.github.diiafyra.motoman.ui.format

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import io.github.diiafyra.motoman.R
import io.github.diiafyra.motoman.core.domain.model.FuelType
import io.github.diiafyra.motoman.core.domain.model.VehicleType
import java.text.NumberFormat

/** Whole numbers with the separator of the current language: 12,000 in English, 12.000 in Vietnamese. */
@Composable
fun formatInteger(value: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return NumberFormat.getIntegerInstance(locale).format(value)
}

@get:StringRes
val VehicleType.labelRes: Int
    get() =
        when (this) {
            VehicleType.MOTORBIKE -> R.string.vehicle_type_motorbike
            VehicleType.CAR -> R.string.vehicle_type_car
        }

@get:StringRes
val FuelType.labelRes: Int
    get() =
        when (this) {
            FuelType.RON95 -> R.string.fuel_type_ron95
            FuelType.E5_RON92 -> R.string.fuel_type_e5_ron92
            FuelType.DIESEL -> R.string.fuel_type_diesel
            FuelType.OTHER -> R.string.fuel_type_other
        }
