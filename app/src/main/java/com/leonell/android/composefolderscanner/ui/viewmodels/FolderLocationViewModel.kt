package com.leonell.android.composefolderscanner.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leonell.android.composefolderscanner.database.FolderLocationRepository
import com.leonell.android.composefolderscanner.database.model.asExternalModel
import com.leonell.android.composefolderscanner.models.BarcodeEntityModel
import com.leonell.android.composefolderscanner.models.ScanModel
import com.leonell.android.composefolderscanner.models.asDatabaseModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FolderLocationUiState {
   data object Loading : FolderLocationUiState
   data object Success : FolderLocationUiState
   data class Error(val message: String) : FolderLocationUiState
}

data class CurrentLocationState(
   val location: BarcodeEntityModel.FolderLocation? = null,
   val currentPerson: BarcodeEntityModel.Person? = null,
   val search: String = "",
)

@HiltViewModel
class FolderLocationViewModel @Inject constructor(
   private val folderLocationRepo: FolderLocationRepository,
) : ViewModel() {

   private val mutableUiState =
      MutableStateFlow<FolderLocationUiState>(FolderLocationUiState.Loading)
   val uiState: StateFlow<FolderLocationUiState> = mutableUiState.asStateFlow()

   private val currentLocationData = MutableStateFlow(CurrentLocationState())
   val currentLocationState: StateFlow<CurrentLocationState> = currentLocationData.asStateFlow()

   /**
    * Locations matching the current search text.
    *
    * The old version captured the search term inside a `map` on a single flow, so typing did
    * not re-filter -- the database flow had to emit again for the new term to be applied.
    * Combining the two makes the search term an actual input.
    */
   val locations: StateFlow<List<BarcodeEntityModel.FolderLocation>> =
      combine(
         folderLocationRepo.getFolderLocationEntities(),
         currentLocationData.map { it.search }.distinctUntilChanged(),
      ) { entities, search ->
         entities
            .asSequence()
            .map { it.asExternalModel() }
            .filter { search.isBlank() || it.name.contains(search, ignoreCase = true) }
            .toList()
            .also { if (it.isNotEmpty()) mutableUiState.value = FolderLocationUiState.Success }
      }.stateIn(
         scope = viewModelScope,
         started = SharingStarted.WhileSubscribed(5_000),
         initialValue = emptyList(),
      )

   init {
      viewModelScope.launch {
         // Backfill the local cache from the server when it is empty.
         folderLocationRepo.getFolderLocationsREST()
      }
   }

   fun setLocation(folder: BarcodeEntityModel.FolderLocation?) =
      currentLocationData.update { it.copy(location = folder) }

   fun saveLocation(location: BarcodeEntityModel.FolderLocation) {
      viewModelScope.launch {
         folderLocationRepo.upsertFolderLocations(listOf(location.asDatabaseModel()))
      }
   }

   fun delete(location: BarcodeEntityModel.FolderLocation) {
      if (!location.custom) return
      viewModelScope.launch {
         folderLocationRepo.deleteFolderLocations(listOf(location.id.toString()))
      }
   }

   fun search(query: String) = currentLocationData.update { it.copy(search = query) }

   /** Resolves a scanned barcode and applies whatever it turns out to be. */
   fun lookup(barcode: ScanModel) {
      viewModelScope.launch {
         val result = folderLocationRepo.lookup(barcode) ?: return@launch
         when (val entity = result.entityObject) {
            is BarcodeEntityModel.FolderLocation ->
               currentLocationData.update { it.copy(location = entity) }

            is BarcodeEntityModel.Person ->
               currentLocationData.update { it.copy(currentPerson = entity) }

            else -> Unit
         }
      }
   }

   fun logOneLocation(barcode: String, locationId: Long) {
      viewModelScope.launch {
         mutableUiState.value = when (folderLocationRepo.logOneLocation(barcode, locationId)) {
            200 -> FolderLocationUiState.Success
            401, 403 -> FolderLocationUiState.Error(
               "Bad credentials. Check the server settings.",
            )

            else -> FolderLocationUiState.Error("There was a problem logging the folders.")
         }
      }
   }
}
