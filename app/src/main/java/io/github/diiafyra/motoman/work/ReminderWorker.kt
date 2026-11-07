package io.github.diiafyra.motoman.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** Runs [ReminderChecker] once a day. Everything it needs is local, so it has no network constraint. */
@HiltWorker
class ReminderWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val checker: ReminderChecker,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            checker.check()
            return Result.success()
        }

        companion object {
            private const val WORK_NAME = "maintenance_reminders"
            private val REMIND_AT: LocalTime = LocalTime.of(9, 0)

            /**
             * Schedules the daily check. KEEP leaves an existing schedule alone, so calling this on every
             * start does not push the next run back.
             */
            fun schedule(
                context: Context,
                now: ZonedDateTime = ZonedDateTime.now(),
            ) {
                val request =
                    PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                        .setInitialDelay(delayUntil(REMIND_AT, now).toMinutes(), TimeUnit.MINUTES)
                        .build()
                WorkManager
                    .getInstance(context)
                    .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
            }

            /** Time until the next [at] after [now]: later today, or tomorrow if it has passed. */
            internal fun delayUntil(
                at: LocalTime,
                now: ZonedDateTime,
            ): Duration {
                val today = now.with(at)
                val next = if (today.isAfter(now)) today else today.plusDays(1)
                return Duration.between(now, next)
            }
        }
    }
