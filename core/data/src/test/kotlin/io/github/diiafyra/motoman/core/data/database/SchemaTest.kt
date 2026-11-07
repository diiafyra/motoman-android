package io.github.diiafyra.motoman.core.data.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opens the database from the exported schema file and then through Room, which fails if the two
 * differ. Each future migration adds a test here that creates the old version and migrates it.
 */
@RunWith(AndroidJUnit4::class)
class SchemaTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), MotoManDatabase::class.java)

    @Test
    fun exportedSchemaMatchesTheEntities() {
        helper.createDatabase(DB_NAME, 1).close()

        Room
            .databaseBuilder(ApplicationProvider.getApplicationContext(), MotoManDatabase::class.java, DB_NAME)
            .build()
            .apply { openHelper.writableDatabase }
            .close()
    }

    private companion object {
        const val DB_NAME = "schema-test.db"
    }
}
