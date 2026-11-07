package io.github.diiafyra.motoman.core.data.time

import io.github.diiafyra.motoman.core.domain.util.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

internal class SystemClock
    @Inject
    constructor() : Clock {
        override fun now(): Instant = Instant.now()

        override fun today(): LocalDate = LocalDate.now(zone())

        // Read on every call: the user may travel or change the zone while the app runs.
        override fun zone(): ZoneId = ZoneId.systemDefault()
    }
