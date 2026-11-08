package io.github.diiafyra.motoman.work

import io.github.diiafyra.motoman.core.domain.maintenance.Reminder
import io.github.diiafyra.motoman.core.domain.maintenance.currentOdometerKm
import io.github.diiafyra.motoman.core.domain.maintenance.rankByUrgency
import io.github.diiafyra.motoman.core.domain.maintenance.remindersFor
import io.github.diiafyra.motoman.core.domain.maintenance.selectReminders
import io.github.diiafyra.motoman.core.domain.repository.FuelEntryRepository
import io.github.diiafyra.motoman.core.domain.repository.MaintenanceRepository
import io.github.diiafyra.motoman.core.domain.repository.MaintenanceSettings
import io.github.diiafyra.motoman.core.domain.repository.PreferencesRepository
import io.github.diiafyra.motoman.core.domain.repository.VehicleRepository
import io.github.diiafyra.motoman.core.domain.util.Clock
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Shows reminders. Implemented with system notifications; replaced by a fake in tests. */
interface ReminderNotifier {
    /** False when the user has not allowed notifications, or turned off the channel. */
    fun canNotify(): Boolean

    fun notify(reminders: List<Reminder>)
}

/**
 * The daily check behind [ReminderWorker], kept out of the worker so it can be tested with fakes.
 * Looks at every vehicle, not only the selected one: an overdue item on the second motorbike matters
 * just as much.
 */
class ReminderChecker
    @Inject
    constructor(
        private val vehicles: VehicleRepository,
        private val fuel: FuelEntryRepository,
        private val maintenance: MaintenanceRepository,
        private val preferences: PreferencesRepository,
        private val settings: MaintenanceSettings,
        private val clock: Clock,
        private val notifier: ReminderNotifier,
    ) {
        /** Returns how many notifications were shown. */
        suspend fun check(): Int {
            // Without permission, keep the remembered keys as they are: these reminders are shown once
            // notifications are allowed.
            if (!preferences.observeRemindersEnabled().first() || !notifier.canNotify()) return 0

            val today = clock.today()
            val threshold = settings.observeDueSoonThreshold().first()
            val current =
                vehicles.observeVehicles().first().flatMap { vehicle ->
                    val items = maintenance.observeItems(vehicle.id).first()
                    if (items.isEmpty()) return@flatMap emptyList()
                    val odometer =
                        currentOdometerKm(
                            vehicle,
                            fuel.observeEntries(vehicle.id).first(),
                            maintenance.observeRecords(vehicle.id).first(),
                        )
                    remindersFor(vehicle, rankByUrgency(items, odometer, today, threshold))
                }
            val (toSend, keys) = selectReminders(current, preferences.sentReminderKeys())
            if (toSend.isNotEmpty()) notifier.notify(toSend)
            preferences.setSentReminderKeys(keys)
            return toSend.size
        }
    }
