package io.github.diiafyra.motoman.core.domain.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The only source of "now". Injected so that tests control time. */
public interface Clock {
    public fun now(): Instant

    /** Today's date in [zone]. */
    public fun today(): LocalDate

    /** The user's time zone, for turning dates the user picks into instants and back. */
    public fun zone(): ZoneId
}
