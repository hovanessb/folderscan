package com.leonell.android.composefolderscanner.ui.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leonell.android.composefolderscanner.models.BarcodeEntityModel
import com.leonell.android.composefolderscanner.services.WebApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
   data object Ready : ProfileUiState
   data object Loading : ProfileUiState
   data class Success(
      val person: BarcodeEntityModel.Person,
      val staffAssignment: List<BarcodeEntityModel.StaffAssignment>,
   ) : ProfileUiState

   data class Error(val message: String) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
   private val webApi: WebApi,
) : ViewModel() {

   var search by mutableStateOf("")
      private set

   private val mutableUiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Ready)
   val uiState: StateFlow<ProfileUiState> = mutableUiState.asStateFlow()

   @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
   val searchPerson: StateFlow<List<BarcodeEntityModel.Person>> =
      snapshotFlow { search }
         .debounce(SEARCH_DEBOUNCE_MS)
         .mapLatest { query ->
            // An empty box should clear the results, not query the whole directory.
            if (query.isBlank()) emptyList()
            else webApi.getPersonWebService().searchPerson(query)
         }
         .catch { exception ->
            Log.e(TAG, "Person search failed", exception)
            emit(emptyList())
         }
         .stateIn(
            scope = viewModelScope,
            // Eagerly kept the search alive for the whole ViewModel; this releases it a few
            // seconds after the screen goes away.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
         )

   fun getPerson(barcodeId: String?, associateId: String?) {
      viewModelScope.launch {
         mutableUiState.value = ProfileUiState.Loading
         try {
            val service = webApi.getPersonWebService()
            val person = when {
               barcodeId != null -> service.getPersonByBarcode(barcodeId)
               associateId != null -> service.getPersonByAssociateId(associateId)
               else -> null
            }

            val personId = person?.id
            mutableUiState.value = if (person != null && personId != null) {
               ProfileUiState.Success(person, service.getAssignedStaff(personId))
            } else {
               ProfileUiState.Error("No profile found for that folder.")
            }
         } catch (exception: Exception) {
            Log.e(TAG, "Could not load profile", exception)
            mutableUiState.value = ProfileUiState.Error("Couldn't retrieve the profile...")
         }
      }
   }

   fun updateSearch(name: String) {
      search = name
   }

   /** Absolute URL of a person's photo, resolved against the configured server. */
   fun photoUrl(personId: Number?): String? =
      personId?.let { webApi.absoluteUrl("leonell/server/proc/photo/$it") }

   private companion object {
      const val TAG = "ProfileViewModel"
      const val SEARCH_DEBOUNCE_MS = 500L
   }
}
