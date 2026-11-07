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

        // Read the zone on every call: the user may travel or change it while the app runs.
        override fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())
    }
