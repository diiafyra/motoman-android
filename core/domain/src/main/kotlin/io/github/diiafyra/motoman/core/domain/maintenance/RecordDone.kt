package io.github.diiafyra.motoman.core.domain.maintenance

import io.github.diiafyra.motoman.core.domain.model.MaintenanceItem
import io.github.diiafyra.motoman.core.domain.model.MaintenanceRecord

/**
 * The item after [record] is logged. Its last-done values move to the record only when the record is
 * the latest service known: a record entered afterwards for an older service must not reset the due
 * point backwards.
 *
 * "Latest" compares dates first and odometer readings on the same date. An item whose last-done value
 * was entered as a reading only is compared by reading.
 */
public fun MaintenanceItem.withRecord(record: MaintenanceRecord): MaintenanceItem {
    require(record.itemId == id) { "The record belongs to another item" }

    val isLatest =
        when {
            lastDoneDate != null -> {
                record.doneDate > lastDoneDate ||
                    (record.doneDate == lastDoneDate && record.odometerKm >= (lastDoneKm ?: 0))
            }

            lastDoneKm != null -> {
                record.odometerKm >= lastDoneKm
            }

            else -> {
                true
            }
        }
    return if (isLatest) copy(lastDoneKm = record.odometerKm, lastDoneDate = record.doneDate) else this
}
