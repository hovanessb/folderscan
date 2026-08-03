package com.leonell.android.composefolderscanner.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.leonell.android.composefolderscanner.data.settings.ServerCredentials
import com.leonell.android.composefolderscanner.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SettingsUiState(
   val baseUrl: String = "",
   val username: String = "",
   val password: String = "",
   /** False when the device keystore was unusable and credentials are stored in the clear. */
   val storageEncrypted: Boolean = true,
   val savedMessage: String? = null,
) {
   val canSave: Boolean
      get() = baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
   private val settings: SettingsRepository,
) : ViewModel() {

   private val mutableUiState = MutableStateFlow(
      settings.credentials.value.let {
         SettingsUiState(
            baseUrl = it.baseUrl,
            username = it.username,
            password = it.password,
            storageEncrypted = settings.isStorageEncrypted,
         )
      },
   )
   val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

   fun updateBaseUrl(value: String) =
      mutableUiState.update { it.copy(baseUrl = value, savedMessage = null) }

   fun updateUsername(value: String) =
      mutableUiState.update { it.copy(username = value, savedMessage = null) }

   fun updatePassword(value: String) =
      mutableUiState.update { it.copy(password = value, savedMessage = null) }

   fun save() {
      val state = mutableUiState.value
      if (!state.canSave) return
      settings.save(
         ServerCredentials(
            baseUrl = state.baseUrl,
            username = state.username,
            password = state.password,
         ),
      )
      mutableUiState.update { it.copy(savedMessage = "Credentials saved.") }
   }

   fun clear() {
      settings.clear()
      val defaults = settings.credentials.value
      mutableUiState.value = SettingsUiState(
         baseUrl = defaults.baseUrl,
         username = defaults.username,
         password = defaults.password,
         storageEncrypted = settings.isStorageEncrypted,
         savedMessage = "Credentials cleared.",
      )
   }

   fun consumeMessage() = mutableUiState.update { it.copy(savedMessage = null) }
}
