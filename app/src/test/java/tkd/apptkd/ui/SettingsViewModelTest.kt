package tkd.apptkd.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import tkd.apptkd.data.repository.SessionRepository
import tkd.apptkd.data.room.AppDatabase
import tkd.apptkd.data.service.DataErasureService
import tkd.apptkd.ui.screens.SettingsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: UserSettingsRepository
    private lateinit var erasureService: DataErasureService
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = UserSettingsRepository(context)
        val sessionRepository = SessionRepository(db)
        val holder = PracticeSessionHolder()
        erasureService = DataErasureService(
            context = context,
            sessionRepository = sessionRepository,
            userSettingsRepository = repository,
            practiceSessionHolder = holder,
        )
        viewModel = SettingsViewModel(
            userSettingsRepository = repository,
            dataErasureService = erasureService,
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testSettingsEmissionsAndUpdateFlow() = runTest(testDispatcher) {
        viewModel.userSettings.test {
            val initial = awaitItem()
            assertEquals(CameraFacing.FRONT, initial.defaultCameraFacing)

            viewModel.setDefaultCameraFacing(CameraFacing.BACK)
            val updatedFacing = awaitItem()
            assertEquals(CameraFacing.BACK, updatedFacing.defaultCameraFacing)

            viewModel.setVoiceEnabled(false)
            val updatedVoice = awaitItem()
            assertFalse(updatedVoice.voiceEnabled)

            viewModel.setExigencyLevel(ExigencyLevel.STRICT)
            val updatedExigency = awaitItem()
            assertEquals(ExigencyLevel.STRICT, updatedExigency.exigencyLevel)

            viewModel.setStrictMode(true)
            val updatedStrict = awaitItem()
            assertTrue(updatedStrict.strictMode)

            viewModel.setSaveSnapshots(true)
            val updatedSnapshots = awaitItem()
            assertTrue(updatedSnapshots.saveSnapshots)
        }
    }

    @Test
    fun testDeleteAllDataInvokesCallback() = runTest(testDispatcher) {
        var callbackCalled = false
        viewModel.deleteAllData {
            callbackCalled = true
        }

        repeat(50) {
            advanceUntilIdle()
            if (callbackCalled) return@repeat
            Thread.sleep(50L)
            advanceUntilIdle()
        }
        assertTrue(callbackCalled)
    }
}
