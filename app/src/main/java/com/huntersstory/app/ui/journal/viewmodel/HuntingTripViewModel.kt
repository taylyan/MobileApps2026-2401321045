
package com.huntersstory.app.ui.journal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.huntersstory.app.data.local.HunterDatabase
import com.huntersstory.app.data.local.entity.HuntingTrip
import com.huntersstory.app.data.repository.HuntingTripRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class HuntingTripViewModel
    @JvmOverloads
    constructor(
        application: Application,
        private val repository: HuntingTripRepository =
            HuntingTripRepository(
                HunterDatabase
                    .getDatabase(application)
                    .huntingTripDao(),
            ),
    ) : AndroidViewModel(application) {
        fun getTripsByUser(userId: String): Flow<List<HuntingTrip>> = repository.getTripsByUser(userId)

        // Repository failures are reported through the UI callback.
        // Coroutine cancellation must propagate normally.
        @Suppress("TooGenericExceptionCaught")
        fun saveTrip(
            trip: HuntingTrip,
            onSuccess: () -> Unit,
            onError: (String) -> Unit,
        ) {
            viewModelScope.launch {
                try {
                    repository.insertTrip(trip)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    onError(
                        e.localizedMessage ?: "Failed to save trip.",
                    )
                    return@launch
                }

                onSuccess()
            }
        }

        fun updateTrip(trip: HuntingTrip) {
            viewModelScope.launch {
                repository.updateTrip(trip)
            }
        }

        fun deleteTrip(trip: HuntingTrip) {
            viewModelScope.launch {
                repository.deleteTrip(trip)
            }
        }
    }
