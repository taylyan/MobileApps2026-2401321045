
package com.huntersstory.app.testing

import android.app.Application
import com.huntersstory.app.data.local.entity.HuntingPhoto
import com.huntersstory.app.data.local.entity.HuntingTrip
import com.huntersstory.app.data.repository.CloudBackupRepository
import com.huntersstory.app.data.repository.CollectionItem
import com.huntersstory.app.data.repository.CollectionRepository
import com.huntersstory.app.data.repository.CollectionType
import com.huntersstory.app.data.repository.HuntingTripRepository
import com.huntersstory.app.data.repository.PhotoRepository
import com.huntersstory.app.ui.collection.CollectionViewModel
import com.huntersstory.app.ui.gallery.GalleryViewModel
import com.huntersstory.app.ui.journal.viewmodel.HuntingTripViewModel
import com.huntersstory.app.ui.sync.CloudSyncViewModel
import com.huntersstory.app.ui.sync.SyncState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelCoverageTest {

    private val application = mockk<Application>(relaxed = true)
    private val uid = "test-user"

    private val sampleItem = CollectionItem(
        id = 10L,
        name = "Binoculars",
        secondary = "Optics",
        notes = "10x42"
    )

    private val sampleTrip = HuntingTrip(
        id = 5L,
        userId = uid,
        date = "2026-11-22",
        location = "Kuklen",
        gameSpecies = "Pheasant",
        notes = "Morning trip"
    )

    private val samplePhoto = HuntingPhoto(
        id = 3L,
        userId = uid,
        fileName = "photo.jpg",
        createdAt = 1000L
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    // ==================================
    // COLLECTION VIEWMODEL
    // ==================================

    @Test
    fun collectionObserveReturnsEquipment() = runTest {
        val repository = mockk<CollectionRepository>()

        every {
            repository.observe(CollectionType.EQUIPMENT, uid)
        } returns flowOf(listOf(sampleItem))

        val viewModel = CollectionViewModel(
            application, repository
        )

        val result = viewModel.observe(
            CollectionType.EQUIPMENT, uid
        ).first()

        assertEquals(1, result.size)
        assertEquals("Binoculars", result[0].name)
    }

    @Test
    fun collectionObserveReturnsEmptyDogs() = runTest {
        val repository = mockk<CollectionRepository>()

        every {
            repository.observe(CollectionType.DOGS, uid)
        } returns flowOf(emptyList())

        val viewModel = CollectionViewModel(
            application, repository
        )

        assertTrue(
            viewModel.observe(
                CollectionType.DOGS, uid
            ).first().isEmpty()
        )
    }

    @Test
    fun collectionSaveSuccessCallsCallback() = runTest {
        val repository = mockk<CollectionRepository>()

        coEvery {
            repository.save(
                CollectionType.EQUIPMENT,
                uid,
                sampleItem
            )
        } returns Unit

        val viewModel = CollectionViewModel(
            application, repository
        )

        var success = false
        var error: String? = null

        viewModel.save(
            CollectionType.EQUIPMENT,
            uid,
            sampleItem,
            onSuccess = { success = true },
            onError = { error = it }
        )

        assertTrue(success)
        assertNull(error)

        coVerify(exactly = 1) {
            repository.save(
                CollectionType.EQUIPMENT,
                uid,
                sampleItem
            )
        }
    }

    @Test
    fun collectionSaveFailureCallsError() = runTest {
        val repository = mockk<CollectionRepository>()

        coEvery {
            repository.save(any(), any(), any())
        } throws IllegalStateException("Save error")

        val viewModel = CollectionViewModel(
            application, repository
        )

        var success = false
        var error: String? = null

        viewModel.save(
            CollectionType.DOGS,
            uid,
            sampleItem,
            onSuccess = { success = true },
            onError = { error = it }
        )

        assertFalse(success)
        assertEquals("Save error", error)
    }

    @Test
    fun collectionDeleteSuccessCallsCallback() = runTest {
        val repository = mockk<CollectionRepository>()

        coEvery {
            repository.delete(
                CollectionType.EQUIPMENT,
                uid,
                10L
            )
        } returns Unit

        val viewModel = CollectionViewModel(
            application, repository
        )

        var success = false
        var error: String? = null

        viewModel.delete(
            CollectionType.EQUIPMENT,
            uid,
            10L,
            onSuccess = { success = true },
            onError = { error = it }
        )

        assertTrue(success)
        assertNull(error)
    }

    @Test
    fun collectionDeleteFailureCallsError() = runTest {
        val repository = mockk<CollectionRepository>()

        coEvery {
            repository.delete(any(), any(), any())
        } throws IllegalStateException("Delete error")

        val viewModel = CollectionViewModel(
            application, repository
        )

        var success = false
        var error: String? = null

        viewModel.delete(
            CollectionType.DOGS,
            uid,
            99L,
            onSuccess = { success = true },
            onError = { error = it }
        )

        assertFalse(success)
        assertEquals("Delete error", error)
    }

    // ==================================
    // HUNTING JOURNAL VIEWMODEL
    // ==================================

    @Test
    fun journalObserveReturnsTrips() = runTest {
        val repository = mockk<HuntingTripRepository>()

        every {
            repository.getTripsByUser(uid)
        } returns flowOf(listOf(sampleTrip))

        val viewModel = HuntingTripViewModel(
            application, repository
        )

        val result = viewModel.getTripsByUser(uid).first()

        assertEquals(1, result.size)
        assertEquals("Kuklen", result[0].location)
    }

    @Test
    fun journalSaveSuccessCallsCallback() = runTest {
        val repository = mockk<HuntingTripRepository>()

        coEvery {
            repository.insertTrip(sampleTrip)
        } returns 5L

        val viewModel = HuntingTripViewModel(
            application, repository
        )

        var success = false
        var error: String? = null

        viewModel.saveTrip(
            sampleTrip,
            onSuccess = { success = true },
            onError = { error = it }
        )

        assertTrue(success)
        assertNull(error)
    }

    @Test
    fun journalSaveFailureCallsError() = runTest {
        val repository = mockk<HuntingTripRepository>()

        coEvery {
            repository.insertTrip(sampleTrip)
        } throws IllegalStateException("Journal error")

        val viewModel = HuntingTripViewModel(
            application, repository
        )

        var success = false
        var error: String? = null

        viewModel.saveTrip(
            sampleTrip,
            onSuccess = { success = true },
            onError = { error = it }
        )

        assertFalse(success)
        assertEquals("Journal error", error)
    }

    @Test
    fun journalUpdateCallsRepository() = runTest {
        val repository = mockk<HuntingTripRepository>()

        coEvery {
            repository.updateTrip(sampleTrip)
        } returns Unit

        val viewModel = HuntingTripViewModel(
            application, repository
        )

        viewModel.updateTrip(sampleTrip)

        coVerify(exactly = 1) {
            repository.updateTrip(sampleTrip)
        }
    }

    @Test
    fun journalDeleteCallsRepository() = runTest {
        val repository = mockk<HuntingTripRepository>()

        coEvery {
            repository.deleteTrip(sampleTrip)
        } returns Unit

        val viewModel = HuntingTripViewModel(
            application, repository
        )

        viewModel.deleteTrip(sampleTrip)

        coVerify(exactly = 1) {
            repository.deleteTrip(sampleTrip)
        }
    }


    // ==================================
    // GALLERY VIEWMODEL
    // ==================================

    @Test
    fun galleryObserveReturnsPhotos() = runTest {
        val repository = mockk<PhotoRepository>()

        every {
            repository.observe(uid)
        } returns flowOf(listOf(samplePhoto))

        val viewModel = GalleryViewModel(
            application, repository
        )

        val result = viewModel.observe(uid).first()

        assertEquals(1, result.size)
        assertEquals("photo.jpg", result[0].fileName)
    }

    @Test
    fun galleryPhotoFileUsesRepository() {
        val repository = mockk<PhotoRepository>()
        val expected = File("photos/photo.jpg")

        every {
            repository.getFile(samplePhoto)
        } returns expected

        val viewModel = GalleryViewModel(
            application, repository
        )

        assertEquals(
            expected,
            viewModel.photoFile(samplePhoto)
        )
    }

    @Test
    fun gallerySaveSuccessCallsCallback() = runTest {
        val repository = mockk<PhotoRepository>()
        val completed = CompletableDeferred<Unit>()

        coEvery {
            repository.save(uid, "photo.jpg")
        } returns Unit

        val viewModel = GalleryViewModel(
            application = application,
            repository = repository,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        var success = false
        var error: String? = null

        viewModel.save(
            uid,
            "photo.jpg",
            onSuccess = {
                success = true
                completed.complete(Unit)
            },
            onError = {
                error = it
                completed.complete(Unit)
            }
        )

        withTimeout(5_000) {
            completed.await()
        }

        assertTrue(success)
        assertNull(error)

        coVerify(exactly = 1) {
            repository.save(uid, "photo.jpg")
        }
    }

    @Test
    fun gallerySaveFailureCallsError() = runTest {
        val repository = mockk<PhotoRepository>()
        val completed = CompletableDeferred<Unit>()

        coEvery {
            repository.save(uid, "missing.jpg")
        } throws IllegalStateException("Missing photo")

        val viewModel = GalleryViewModel(
            application = application,
            repository = repository,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        var success = false
        var error: String? = null

        viewModel.save(
            uid,
            "missing.jpg",
            onSuccess = {
                success = true
                completed.complete(Unit)
            },
            onError = {
                error = it
                completed.complete(Unit)
            }
        )

        withTimeout(5_000) {
            completed.await()
        }

        assertFalse(success)
        assertEquals("Missing photo", error)

        coVerify(exactly = 1) {
            repository.save(uid, "missing.jpg")
        }
    }

    @Test
    fun galleryDeleteSuccessCallsCallback() = runTest {
        val repository = mockk<PhotoRepository>()
        val completed = CompletableDeferred<Unit>()

        coEvery {
            repository.delete(samplePhoto, uid)
        } returns Unit

        val viewModel = GalleryViewModel(
            application = application,
            repository = repository,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        var success = false
        var error: String? = null

        viewModel.delete(
            samplePhoto,
            uid,
            onSuccess = {
                success = true
                completed.complete(Unit)
            },
            onError = {
                error = it
                completed.complete(Unit)
            }
        )

        withTimeout(5_000) {
            completed.await()
        }

        assertTrue(success)
        assertNull(error)

        coVerify(exactly = 1) {
            repository.delete(samplePhoto, uid)
        }
    }

    @Test
    fun galleryDeleteFailureCallsError() = runTest {
        val repository = mockk<PhotoRepository>()
        val completed = CompletableDeferred<Unit>()

        coEvery {
            repository.delete(samplePhoto, uid)
        } throws IllegalStateException("Photo not found")

        val viewModel = GalleryViewModel(
            application = application,
            repository = repository,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        var success = false
        var error: String? = null

        viewModel.delete(
            samplePhoto,
            uid,
            onSuccess = {
                success = true
                completed.complete(Unit)
            },
            onError = {
                error = it
                completed.complete(Unit)
            }
        )

        withTimeout(5_000)
        { completed.await()
        }

        assertFalse(success)
        assertEquals("Photo not found", error)

        coVerify(exactly = 1) {
            repository.delete(samplePhoto, uid)
        }
    }


    // ==================================
    // CLOUD SYNC VIEWMODEL
    // ==================================

    @Test
    fun cloudSyncStartsIdle() {
        val repository = mockk<CloudBackupRepository>()
        val viewModel = CloudSyncViewModel(
            application, repository
        )

        assertEquals(SyncState.Idle, viewModel.state.value)
    }

    @Test
    fun cloudUploadSuccessUpdatesState() = runTest {
        val repository = mockk<CloudBackupRepository>()

        coEvery {
            repository.upload(uid)
        } returns Unit

        val viewModel = CloudSyncViewModel(
            application, repository
        )

        viewModel.upload(uid)

        val state = viewModel.state.value

        assertTrue(state is SyncState.Success)
        assertTrue(
            (state as SyncState.Success)
                .message.contains("uploaded")
        )
    }

    @Test
    fun cloudUploadFailureUpdatesState() = runTest {
        val repository = mockk<CloudBackupRepository>()

        coEvery {
            repository.upload(uid)
        } throws IllegalStateException("Network error")

        val viewModel = CloudSyncViewModel(
            application, repository
        )

        viewModel.upload(uid)

        val state = viewModel.state.value

        assertTrue(state is SyncState.Error)
        assertEquals(
            "Network error",
            (state as SyncState.Error).message
        )
    }

    @Test
    fun cloudRestoreSuccessUpdatesState() = runTest {
        val repository = mockk<CloudBackupRepository>()

        coEvery {
            repository.restore(uid)
        } returns Unit

        val viewModel = CloudSyncViewModel(
            application, repository
        )

        viewModel.restore(uid)

        val state = viewModel.state.value

        assertTrue(state is SyncState.Success)
        assertTrue(
            (state as SyncState.Success)
                .message.contains("restored")
        )
    }

    @Test
    fun cloudRestoreFailureUpdatesState() = runTest {
        val repository = mockk<CloudBackupRepository>()

        coEvery {
            repository.restore(uid)
        } throws IllegalStateException("No backup")

        val viewModel = CloudSyncViewModel(
            application, repository
        )

        viewModel.restore(uid)

        val state = viewModel.state.value

        assertTrue(state is SyncState.Error)
        assertEquals(
            "No backup",
            (state as SyncState.Error).message
        )
    }
}
