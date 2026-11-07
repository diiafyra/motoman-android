package io.github.diiafyra.motoman.core.domain.util

import java.time.Instant
import java.time.LocalDate

/** The only source of "now". Injected so that tests control time. */
public interface Clock {
    public fun now(): Instant

    /** Today's date in the user's time zone. */
    public fun today(): LocalDate
}
