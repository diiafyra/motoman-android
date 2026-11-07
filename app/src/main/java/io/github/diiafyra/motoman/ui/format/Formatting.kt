package io.github.diiafyra.motoman.ui.format

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import io.github.diiafyra.motoman.R
import io.github.diiafyra.motoman.core.domain.model.FuelType
import io.github.diiafyra.motoman.core.domain.model.VehicleType
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** Whole numbers with the separator of the current language: 12,000 in English, 12.000 in Vietnamese. */
@Composable
fun formatInteger(value: Long): String = NumberFormat.getIntegerInstance(currentLocale()).format(value)

/** Litres from millilitres, up to two decimals: "4.25", "4,25" in Vietnamese, "4" for whole litres. */
@Composable
fun formatLiters(volumeMl: Long): String = formatDecimal(volumeMl / 1_000.0, maxFractionDigits = 2)

@Composable
fun formatDecimal(
    value: Double,
    maxFractionDigits: Int,
): String =
    NumberFormat
        .getNumberInstance(currentLocale())
        .apply { maximumFractionDigits = maxFractionDigits }
        .format(value)

/** "8 Nov 2026" in English, "8 thg 11, 2026" in Vietnamese. */
@Composable
fun formatDate(date: LocalDate): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(currentLocale()).format(date)

/** "November 2026", "Tháng 11 2026". Capitalised, since it is used as a heading. */
@Composable
fun formatMonth(month: YearMonth): String {
    val locale = currentLocale()
    return DateTimeFormatter
        .ofPattern("LLLL yyyy", locale)
        .format(month)
        .replaceFirstChar { it.titlecase(locale) }
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
