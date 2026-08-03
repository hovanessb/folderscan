package com.leonell.android.composefolderscanner.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leonell.android.composefolderscanner.services.WebApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The folder currently being hunted for, and how strongly the reader is hearing it. */
data class GeigerTarget(
   val barcodeId: String = "",
   val displayName: String = "",
   val signal: Float = 0f,
)

/**
 * Backs the Geiger-search screen.
 *
 * The screen used to call `WebApi()` from inside composition -- a fresh OkHttp client, with
 * its own connection pool and thread pool, allocated on every recomposition.
 */
@HiltViewModel
class LocateViewModel @Inject constructor(
   private val webApi: WebApi,
) : ViewModel() {

   private val mutableTarget = MutableStateFlow(GeigerTarget())
   val target: StateFlow<GeigerTarget> = mutableTarget.asStateFlow()

   /** Looks up whose folder [barcodeId] is and makes it the search target. */
   fun selectTarget(barcodeId: String) {
      if (barcodeId.isBlank() || barcodeId == mutableTarget.value.barcodeId) return
      viewModelScope.launch {
         val name = runCatching { webApi.getPersonWebService().getPersonByBarcode(barcodeId) }
            .onFailure { Log.e(TAG, "Could not resolve folder $barcodeId", it) }
            .getOrNull()
            ?.let { "${it.fullName} #${it.addoId}" }
            ?: barcodeId

         mutableTarget.value = GeigerTarget(barcodeId = barcodeId, displayName = name)
      }
   }

   /** Records the latest signal strength, already normalised to 0..1. */
   fun updateSignal(signal: Float) =
      mutableTarget.update { it.copy(signal = signal.coerceIn(0f, 1f)) }

   /** Lets the meter fall back toward zero when reads stop arriving. */
   fun decaySignal(amount: Float = SIGNAL_DECAY) =
      mutableTarget.update { it.copy(signal = (it.signal - amount).coerceAtLeast(0f)) }

   fun clear() {
      mutableTarget.value = GeigerTarget()
   }

   companion object {
      private const val TAG = "LocateViewModel"

      /** dBm offset applied by the reader's RSSI encoding. */
      const val DB_CONSTANT = 106.98f
      const val SIGNAL_MIN_DBM = -90f
      const val SIGNAL_MAX_DBM = -10f
      const val SIGNAL_DECAY = 0.20f
   }
}
