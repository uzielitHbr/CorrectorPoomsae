package tkd.apptkd.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tkd.apptkd.data.room.DatabaseMigrations
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    @Test
    fun testMigration1To2ExecutesAlterTable() {
        val mockDb = mockk<SupportSQLiteDatabase>(relaxed = true)
        DatabaseMigrations.MIGRATION_1_2.migrate(mockDb)

        verify(exactly = 1) {
            mockDb.execSQL("ALTER TABLE session ADD COLUMN notes TEXT DEFAULT NULL")
        }
    }

    @Test
    fun testBackupDatabaseFileCreatesBackupWhenDbExists() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val dbFile = context.getDatabasePath(DatabaseMigrations.DATABASE_NAME)
        dbFile.parentFile?.mkdirs()
        dbFile.writeText("fake-database-content")

        val backupPath = DatabaseMigrations.backupDatabaseFile(context, DatabaseMigrations.DATABASE_NAME)
        assertNotNull(backupPath)
        val backupFile = File(backupPath!!)
        assertTrue(backupFile.exists())
        assertTrue(backupFile.readText() == "fake-database-content")

        // Limpieza
        dbFile.delete()
        backupFile.delete()
    }

    @Test
    fun testBackupDatabaseFileReturnsNullWhenDbDoesNotExist() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val nonExistentName = "non_existent_db_12345.db"
        val result = DatabaseMigrations.backupDatabaseFile(context, nonExistentName)
        assertNull(result)
    }
}

