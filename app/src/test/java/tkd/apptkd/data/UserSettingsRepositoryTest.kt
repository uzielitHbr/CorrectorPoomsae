package tkd.apptkd.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tkd.apptkd.camera.CameraFacing
import tkd.apptkd.data.datastore.ExigencyLevel
import tkd.apptkd.data.datastore.UserSettingsRepository

@RunWith(RobolectricTestRunner::class)
class UserSettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: UserSettingsRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = UserSettingsRepository(context)
        runBlocking { repository.clearAll() }
    }

    @After
    fun tearDown() = runBlocking {
        repository.clearAll()
    }

    @Test
    fun testDefaultSettings() = runBlocking {
        val settings = repository.userSettingsFlow.first()
        assertEquals(CameraFacing.FRONT, settings.defaultCameraFacing)
        assertTrue(settings.voiceEnabled)
        assertEquals(ExigencyLevel.NORMAL, settings.exigencyLevel)
        assertFalse(settings.strictMode)
        assertFalse(settings.saveSnapshots)
    }

    @Test
    fun testUpdateCameraFacingAndVoice() = runBlocking {
        repository.setDefaultCameraFacing(CameraFacing.BACK)
        repository.setVoiceEnabled(false)

        val updated = repository.userSettingsFlow.first()
        assertEquals(CameraFacing.BACK, updated.defaultCameraFacing)
        assertFalse(updated.voiceEnabled)
    }

    @Test
    fun testUpdateExigencyAndStrict() = runBlocking {
        repository.setExigencyLevel(ExigencyLevel.STRICT)
        repository.setStrictMode(true)
        repository.setSaveSnapshots(true)

        val updated = repository.userSettingsFlow.first()
        assertEquals(ExigencyLevel.STRICT, updated.exigencyLevel)
        assertTrue(updated.strictMode)
        assertTrue(updated.saveSnapshots)
    }

    @Test
    fun testClearAllResetsToDefaults() = runBlocking {
        repository.setDefaultCameraFacing(CameraFacing.BACK)
        repository.setStrictMode(true)
        repository.clearAll()

        val reset = repository.userSettingsFlow.first()
        assertEquals(CameraFacing.FRONT, reset.defaultCameraFacing)
        assertFalse(reset.strictMode)
    }
}

