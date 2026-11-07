package io.github.diiafyra.motoman.core.domain.maintenance

import io.github.diiafyra.motoman.core.domain.model.MaintenanceItem
import io.github.diiafyra.motoman.core.domain.model.Vehicle
import io.github.diiafyra.motoman.core.domain.model.VehicleType
import io.github.diiafyra.motoman.core.domain.util.IdGenerator

/**
 * Items every new vehicle starts with. The intervals are common suggestions for Vietnam, not
 * manufacturer figures: the screen tells the user to check the owner's manual, and every item can be
 * edited or deleted.
 */
public enum class MaintenanceTemplate(
    internal val motorbike: Interval?,
    internal val car: Interval?,
) {
    OIL_CHANGE(motorbike = Interval(km = 2_000, months = 4), car = Interval(km = 5_000, months = 6)),
    AIR_FILTER(motorbike = Interval(km = 8_000, months = 12), car = Interval(km = 15_000, months = 12)),
    TYRES(motorbike = Interval(km = 20_000, months = null), car = Interval(km = 40_000, months = 48)),
    BRAKES(motorbike = Interval(km = 10_000, months = 12), car = Interval(km = 20_000, months = 24)),

    /** Periodic roadworthiness inspection (đăng kiểm). Cars only. */
    INSPECTION(motorbike = null, car = Interval(km = null, months = 12)),
    ;

    internal data class Interval(
        val km: Int?,
        val months: Int?,
    )
}

/** Names the templates in the user's language when the items are created. */
public fun interface TemplateNames {
    public fun nameOf(template: MaintenanceTemplate): String
}

/** The starting items for [vehicle], in template order, with no baseline yet. */
public fun templateItems(
    vehicle: Vehicle,
    names: TemplateNames,
    ids: IdGenerator,
): List<MaintenanceItem> =
    MaintenanceTemplate.entries.mapNotNull { template ->
        val interval =
            when (vehicle.type) {
                VehicleType.MOTORBIKE -> template.motorbike
                VehicleType.CAR -> template.car
            } ?: return@mapNotNull null
        MaintenanceItem(
            id = ids.newId(),
            vehicleId = vehicle.id,
            name = names.nameOf(template),
            intervalKm = interval.km,
            intervalMonths = interval.months,
            lastDoneKm = null,
            lastDoneDate = null,
        )
    }
