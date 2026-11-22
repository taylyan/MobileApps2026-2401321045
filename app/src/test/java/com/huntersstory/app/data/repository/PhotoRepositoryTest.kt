
package com.huntersstory.app.data.repository

import android.content.Context
import com.huntersstory.app.data.local.dao.HuntingPhotoDao
import com.huntersstory.app.data.local.entity.HuntingPhoto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PhotoRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var dao: HuntingPhotoDao
    private lateinit var repository: PhotoRepository
    private lateinit var appFiles: File

    private val uid = "test-user"
    private val otherUid = "another-user"

    private val photo =
        HuntingPhoto(
            id = 15L,
            userId = uid,
            fileName = "hunt.jpg",
            createdAt = 1000L,
        )

    @Before
    fun setup() {
        appFiles = tempFolder.newFolder("app-files")

        context = mockk()
        dao = mockk()

        every {
            context.applicationContext
        } returns context

        every {
            context.filesDir
        } returns appFiles

        repository = PhotoRepository(context, dao)
    }

    private fun createPhoto(
        fileName: String = "hunt.jpg",
        content: String = "Sample photo data",
    ): File {
        val directory = File(appFiles, "photos")
        directory.mkdirs()

        return File(directory, fileName).apply {
            writeText(content)
        }
    }

    // ==================================
    // PHOTO DIRECTORY AND FILE PATHS
    // ==================================

    @Test
    fun photoDirectoryIsCreated() {
        val directory = repository.photoDirectory

        assertTrue(directory.exists())
        assertTrue(directory.isDirectory)
        assertEquals("photos", directory.name)
        assertEquals(appFiles, directory.parentFile)
    }

    @Test
    fun photoDirectoryCanBeAccessedMultipleTimes() {
        val first = repository.photoDirectory
        val second = repository.photoDirectory

        assertEquals(first, second)
        assertTrue(second.isDirectory)
    }

    @Test
    fun getFileReturnsCorrectPath() {
        val file = repository.getFile(photo)

        assertEquals(
            File(appFiles, "photos/hunt.jpg"),
            file,
        )
    }

    // ==================================
    // OBSERVE PHOTOS
    // ==================================

    @Test
    fun observeReturnsPhotosForUser() =
        runBlocking {
            every {
                dao.getByUser(uid)
            } returns flowOf(listOf(photo))

            val result = repository.observe(uid).first()

            assertEquals(1, result.size)
            assertEquals("hunt.jpg", result[0].fileName)
            assertEquals(uid, result[0].userId)
        }

    @Test
    fun observeReturnsEmptyList() =
        runBlocking {
            every {
                dao.getByUser(uid)
            } returns flowOf(emptyList())

            val result = repository.observe(uid).first()

            assertTrue(result.isEmpty())
        }

    // ==================================
    // SAVE PHOTO
    // ==================================

    @Test
    fun saveValidPhotoInsertsRecord() =
        runBlocking {
            createPhoto()

            coEvery {
                dao.insert(any())
            } returns 15L

            repository.save(uid, "hunt.jpg")

            coVerify(exactly = 1) {
                dao.insert(
                    match {
                        it.userId == uid &&
                            it.fileName == "hunt.jpg" &&
                            it.createdAt > 0L
                    },
                )
            }
        }

    @Test
    fun saveMissingPhotoIsRejected() =
        runBlocking {
            val error =
                runCatching {
                    repository.save(uid, "missing.jpg")
                }.exceptionOrNull()

            assertTrue(error is IllegalArgumentException)
            assertEquals(
                "Photo file is missing or empty.",
                error?.message,
            )

            coVerify(exactly = 0) {
                dao.insert(any())
            }
        }

    @Test
    fun saveEmptyPhotoIsRejected() =
        runBlocking {
            createPhoto(content = "")

            val error =
                runCatching {
                    repository.save(uid, "hunt.jpg")
                }.exceptionOrNull()

            assertTrue(error is IllegalArgumentException)

            coVerify(exactly = 0) {
                dao.insert(any())
            }
        }

    @Test
    fun saveRejectsDirectoryInsteadOfPhoto() =
        runBlocking {
            val directory =
                File(
                    repository.photoDirectory,
                    "not-a-photo.jpg",
                )
            directory.mkdirs()

            val error =
                runCatching {
                    repository.save(uid, "not-a-photo.jpg")
                }.exceptionOrNull()

            assertTrue(error is IllegalArgumentException)

            coVerify(exactly = 0) {
                dao.insert(any())
            }
        }

    @Test
    fun savePropagatesDatabaseError() =
        runBlocking {
            createPhoto()

            coEvery {
                dao.insert(any())
            } throws IllegalStateException("Database error")

            val error =
                runCatching {
                    repository.save(uid, "hunt.jpg")
                }.exceptionOrNull()

            assertTrue(error is IllegalStateException)
            assertEquals("Database error", error?.message)
        }

    // ==================================
    // DELETE PHOTO
    // ==================================

    @Test
    fun deleteRemovesRecordAndFile() =
        runBlocking {
            val file = createPhoto()

            coEvery {
                dao.deleteById(15L, uid)
            } returns 1

            assertTrue(file.exists())

            repository.delete(photo, uid)

            coVerify(exactly = 1) {
                dao.deleteById(15L, uid)
            }

            assertFalse(file.exists())
        }

    @Test
    fun deleteWorksWhenFileAlreadyMissing() =
        runBlocking {
            coEvery {
                dao.deleteById(15L, uid)
            } returns 1

            repository.delete(photo, uid)

            coVerify(exactly = 1) {
                dao.deleteById(15L, uid)
            }

            assertFalse(repository.getFile(photo).exists())
        }

    @Test
    fun deleteMissingRecordThrowsError() =
        runBlocking {
            coEvery {
                dao.deleteById(15L, uid)
            } returns 0

            val error =
                runCatching {
                    repository.delete(photo, uid)
                }.exceptionOrNull()

            assertTrue(error is IllegalStateException)
            assertEquals("Photo not found.", error?.message)
        }

    @Test
    fun cannotDeleteAnotherUsersPhoto() =
        runBlocking {
            val file = createPhoto()

            coEvery {
                dao.deleteById(15L, otherUid)
            } returns 0

            val error =
                runCatching {
                    repository.delete(photo, otherUid)
                }.exceptionOrNull()

            assertTrue(error is IllegalStateException)
            assertTrue(file.exists())

            coVerify(exactly = 1) {
                dao.deleteById(15L, otherUid)
            }
        }

    @Test
    fun deletePropagatesDatabaseError() =
        runBlocking {
            val file = createPhoto()

            coEvery {
                dao.deleteById(15L, uid)
            } throws IllegalStateException("Delete database error")

            val error =
                runCatching {
                    repository.delete(photo, uid)
                }.exceptionOrNull()

            assertTrue(error is IllegalStateException)
            assertEquals(
                "Delete database error",
                error?.message,
            )
            assertTrue(file.exists())
        }
}
