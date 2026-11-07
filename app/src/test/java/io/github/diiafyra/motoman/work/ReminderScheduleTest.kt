package io.github.diiafyra.motoman.work

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

class ReminderScheduleTest {
    private val nine = LocalTime.of(9, 0)

    @Test
    fun `before nine the first run is later today`() {
        val now = ZonedDateTime.parse("2026-11-08T07:30:00+07:00[Asia/Ho_Chi_Minh]")

        assertEquals(Duration.ofMinutes(90), ReminderWorker.delayUntil(nine, now))
    }

    @Test
    fun `at or after nine the first run is tomorrow`() {
        val atNine = ZonedDateTime.parse("2026-11-08T09:00:00+07:00[Asia/Ho_Chi_Minh]")
        val evening = ZonedDateTime.parse("2026-11-08T21:00:00+07:00[Asia/Ho_Chi_Minh]")

        assertEquals(Duration.ofHours(24), ReminderWorker.delayUntil(nine, atNine))
        assertEquals(Duration.ofHours(12), ReminderWorker.delayUntil(nine, evening))
    }
}
