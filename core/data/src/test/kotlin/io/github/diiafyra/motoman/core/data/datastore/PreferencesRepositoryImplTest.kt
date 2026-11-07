package io.github.diiafyra.motoman.core.data.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PreferencesRepositoryImplTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun selectedVehicleIsNullUntilSet() =
        runTest {
            val file = File(folder.root, "test.preferences_pb")
            val repository =
                PreferencesRepositoryImpl(
                    PreferenceDataStoreFactory.create(scope = backgroundScope) { file },
                )
            assertNull(repository.observeSelectedVehicleId().first())

            repository.setSelectedVehicleId("v2")

            assertEquals("v2", repository.observeSelectedVehicleId().first())
        }

    @Test
    fun remindersAreOnByDefaultAndSentKeysAreReplaced() =
        runTest {
            val repository =
                PreferencesRepositoryImpl(
                    PreferenceDataStoreFactory.create(
                        scope = backgroundScope,
                    ) { File(folder.root, "r.preferences_pb") },
                )
            assertEquals(true, repository.observeRemindersEnabled().first())
            assertEquals(false, repository.observeNotificationPromptDismissed().first())
            assertEquals(emptySet<String>(), repository.sentReminderKeys())

            repository.setRemindersEnabled(false)
            repository.setSentReminderKeys(setOf("a", "b"))
            repository.setSentReminderKeys(setOf("c"))
            repository.setNotificationPromptDismissed()

            assertEquals(false, repository.observeRemindersEnabled().first())
            assertEquals(setOf("c"), repository.sentReminderKeys())
            assertEquals(true, repository.observeNotificationPromptDismissed().first())
        }
}
