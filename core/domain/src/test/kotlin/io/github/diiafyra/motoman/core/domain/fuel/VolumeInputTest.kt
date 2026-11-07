package io.github.diiafyra.motoman.core.domain.fuel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VolumeInputTest {
    @Test
    fun `whole litres`() {
        assertEquals(4_000L, parseVolumeMl("4"))
        assertEquals(0L, parseVolumeMl("0"))
    }

    @Test
    fun `dot and comma both work as the decimal separator`() {
        assertEquals(4_250L, parseVolumeMl("4.25"))
        assertEquals(4_250L, parseVolumeMl("4,25"))
    }

    @Test
    fun `decimals are exact to the millilitre`() {
        assertEquals(1_150L, parseVolumeMl("1.15"))
        assertEquals(1_500L, parseVolumeMl("1,5"))
        assertEquals(3_457L, parseVolumeMl("3.457"))
        assertEquals(100L, parseVolumeMl("0.1"))
    }

    @Test
    fun `surrounding spaces are ignored`() {
        assertEquals(4_000L, parseVolumeMl(" 4 "))
    }

    @Test
    fun `anything else is rejected`() {
        listOf("", " ", "4.", ".5", "4.2567", "1,000.5", "4 L", "-3", "abc", "4..2", "1234567").forEach {
            assertNull("\"$it\"", parseVolumeMl(it))
        }
    }
}
