package io.github.diiafyra.motoman.core.domain.maintenance

import io.github.diiafyra.motoman.core.domain.item
import io.github.diiafyra.motoman.core.domain.vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReminderTest {
    private val today = LocalDate.of(2026, 6, 1)
    private val oil = item(intervalKm = 1_000, lastDoneKm = 1_000)

    private fun reminders(currentKm: Int) =
        remindersFor(
            vehicle(),
            rankByUrgency(listOf(oil, oil.copy(id = "fine", intervalKm = 10_000)), currentKm, today),
        )

    @Test
    fun `only overdue and due-soon items become reminders`() {
        assertTrue(reminders(currentKm = 1_100).isEmpty())
        assertEquals(listOf("i1"), reminders(currentKm = 1_600).map { it.itemStatus.item.id })
    }

    @Test
    fun `a reminder is sent once, and remembered keys follow the current reminders`() {
        val first = selectReminders(reminders(1_600), alreadySent = emptySet())
        assertEquals(1, first.first.size)

        val second = selectReminders(reminders(1_650), alreadySent = first.second)
        assertTrue("same due point, already sent", second.first.isEmpty())
        assertEquals(first.second, second.second)
    }

    @Test
    fun `becoming overdue after due soon is notified again`() {
        val soon = selectReminders(reminders(1_600), emptySet())
        val overdue = selectReminders(reminders(2_100), soon.second)

        assertEquals(1, overdue.first.size)
        assertTrue(
            overdue.first
                .single()
                .itemStatus.status is MaintenanceStatus.Overdue,
        )
    }

    @Test
    fun `a new due point after the item is done gets a new key`() {
        val before = reminders(1_600).single().key
        val after =
            remindersFor(
                vehicle(),
                rankByUrgency(listOf(oil.copy(lastDoneKm = 1_700)), 2_600, today),
            ).single().key

        assertNotEquals(before, after)
    }

    @Test
    fun `an item that recovers is forgotten, so it is notified when due again`() {
        val sent = selectReminders(reminders(1_600), emptySet()).second
        val recovered = selectReminders(reminders(1_100), sent)
        assertTrue(recovered.second.isEmpty())

        assertEquals(1, selectReminders(reminders(1_600), recovered.second).first.size)
    }
}
