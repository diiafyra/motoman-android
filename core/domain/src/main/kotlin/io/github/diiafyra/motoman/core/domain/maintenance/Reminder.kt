package io.github.diiafyra.motoman.core.domain.maintenance

import io.github.diiafyra.motoman.core.domain.model.Vehicle

/** A maintenance item worth a notification. */
public data class Reminder(
    val vehicle: Vehicle,
    val itemStatus: ItemStatus,
) {
    /**
     * Identifies this reminder for this due point. Marking the item done moves the due point and so
     * changes the key; going from due soon to overdue changes it too. Each key is notified once.
     */
    val key: String
        get() {
            val due =
                when (val status = itemStatus.status) {
                    is MaintenanceStatus.Overdue -> "overdue|${status.due.dueKm}|${status.due.dueDate}"
                    is MaintenanceStatus.DueSoon -> "soon|${status.due.dueKm}|${status.due.dueDate}"
                    else -> error("Only overdue and due-soon items become reminders")
                }
            return "${itemStatus.item.id}|$due"
        }
}

/** The overdue and due-soon items among [statuses], in the order given. */
public fun remindersFor(
    vehicle: Vehicle,
    statuses: List<ItemStatus>,
): List<Reminder> =
    statuses
        .filter { it.status is MaintenanceStatus.Overdue || it.status is MaintenanceStatus.DueSoon }
        .map { Reminder(vehicle, it) }

/**
 * Which reminders to notify now, and the keys to remember for next time.
 *
 * A reminder is sent once per key. The remembered keys are replaced by the current ones, so the set
 * never grows beyond the items that need attention today, and an item that recovers and later becomes
 * due again is notified again.
 */
public fun selectReminders(
    current: List<Reminder>,
    alreadySent: Set<String>,
): Pair<List<Reminder>, Set<String>> = current.filter { it.key !in alreadySent } to current.map { it.key }.toSet()
