package com.leonell.android.composefolderscanner.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.leonell.android.composefolderscanner.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/** One decode reported by a Zebra DataWedge-managed imager. */
data class DataWedgeScan(
   val data: String,
   val labelType: String?,
   val source: String?,
)

/**
 * Integration with Zebra DataWedge, used by the TC210K (and the older TC21/TC26).
 *
 * The app provisions its own DataWedge profile on start so devices do not have to be staged
 * by hand. Decodes are delivered by **broadcast** rather than "Start Activity": broadcast
 * delivery does not disturb the activity stack, reaches the app in any navigation state, and
 * does not depend on `singleTop` launch semantics.
 */
object DataWedgeService {
   private const val TAG = "DataWedgeService"

   private const val PROFILE_NAME = "INCOMMSETTINGS"
   private const val ACTION_DATAWEDGE = "com.symbol.datawedge.api.ACTION"
   private const val EXTRA_CREATE_PROFILE = "com.symbol.datawedge.api.CREATE_PROFILE"
   private const val EXTRA_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG"

   private const val EXTRA_DATA_STRING = "com.symbol.datawedge.data_string"
   private const val EXTRA_LABEL_TYPE = "com.symbol.datawedge.label_type"
   private const val EXTRA_SOURCE = "com.symbol.datawedge.source"

   /** DataWedge intent delivery mode 2 == broadcast. */
   private const val INTENT_DELIVERY_BROADCAST = "2"

   /**
    * Creates (or updates) the DataWedge profile bound to this package.
    *
    * Safe to call on every launch and on non-Zebra hardware: the broadcasts are simply
    * dropped when DataWedge is not installed.
    */
   suspend fun createDWProfile(context: Context) = withContext(Dispatchers.Default) {
      val action = context.getString(R.string.activity_intent_filter_action)

      sendDataWedgeIntent(context, EXTRA_CREATE_PROFILE, PROFILE_NAME)

      val profileConfig = Bundle().apply {
         putString("PROFILE_NAME", PROFILE_NAME)
         putString("PROFILE_ENABLED", "true") // DataWedge expects strings, not booleans.
         putString("CONFIG_MODE", "UPDATE")
      }

      // Associate the profile with this app, all activities.
      val appConfig = Bundle().apply {
         putString("PACKAGE_NAME", context.packageName)
         putStringArray("ACTIVITY_LIST", arrayOf("*"))
      }
      profileConfig.putParcelableArray("APP_LIST", arrayOf(appConfig))

      // Only one plugin can be configured per SET_CONFIG call.
      profileConfig.putBundle(
         "PLUGIN_CONFIG",
         pluginConfig("BARCODE") {
            putString("scanner_selection", "auto")
            putString("scanner_input_enabled", "true")
            // The TC210K exposes more than one scanner backend; configure them uniformly.
            putString("configure_all_scanners", "true")
         },
      )
      sendDataWedgeIntent(context, EXTRA_SET_CONFIG, profileConfig)

      profileConfig.putBundle(
         "PLUGIN_CONFIG",
         pluginConfig("INTENT") {
            putString("intent_output_enabled", "true")
            putString("intent_action", action)
            putString("intent_delivery", INTENT_DELIVERY_BROADCAST)
         },
      )
      sendDataWedgeIntent(context, EXTRA_SET_CONFIG, profileConfig)

      // Keyboard emulation would double-deliver every scan alongside the intent output.
      profileConfig.putBundle(
         "PLUGIN_CONFIG",
         pluginConfig("KEYSTROKE") {
            putString("keystroke_output_enabled", "false")
         },
      )
      sendDataWedgeIntent(context, EXTRA_SET_CONFIG, profileConfig)
   }

   /**
    * Emits every decode delivered by DataWedge for as long as the flow is collected.
    *
    * The receiver is registered as *exported*: DataWedge is a separate process, so on
    * Android 13 (API 33) -- where a runtime receiver must declare an export flag -- an
    * unexported receiver would silently never fire.
    */
   fun scans(context: Context): Flow<DataWedgeScan> = callbackFlow {
      val appContext = context.applicationContext
      val receiver = object : BroadcastReceiver() {
         override fun onReceive(receiverContext: Context?, intent: Intent?) {
            trySend(scanFromIntent(intent) ?: return)
         }
      }

      val filter = IntentFilter(appContext.getString(R.string.activity_intent_filter_action))
      ContextCompat.registerReceiver(
         appContext,
         receiver,
         filter,
         ContextCompat.RECEIVER_EXPORTED,
      )

      awaitClose { runCatching { appContext.unregisterReceiver(receiver) } }
   }

   /**
    * Reads a decode out of an Intent DataWedge delivered via "Start Activity".
    *
    * Kept for devices still staged with an older, hand-made profile.
    */
   fun scanFromIntent(intent: Intent?): DataWedgeScan? {
      val data = intent?.getStringExtra(EXTRA_DATA_STRING)?.trim()
      if (data.isNullOrBlank()) return null
      return DataWedgeScan(
         data = data,
         labelType = intent.getStringExtra(EXTRA_LABEL_TYPE),
         source = intent.getStringExtra(EXTRA_SOURCE),
      )
   }

   private fun pluginConfig(name: String, params: Bundle.() -> Unit): Bundle =
      Bundle().apply {
         putString("PLUGIN_NAME", name)
         putString("RESET_CONFIG", "true")
         putBundle("PARAM_LIST", Bundle().apply(params))
      }

   private fun sendDataWedgeIntent(context: Context, extraKey: String, extraValue: String) {
      sendDataWedgeIntent(context) { putExtra(extraKey, extraValue) }
   }

   private fun sendDataWedgeIntent(context: Context, extraKey: String, extras: Bundle) {
      sendDataWedgeIntent(context) { putExtra(extraKey, extras) }
   }

   private fun sendDataWedgeIntent(context: Context, configure: Intent.() -> Unit) {
      runCatching {
         context.sendBroadcast(Intent(ACTION_DATAWEDGE).apply(configure))
      }.onFailure { Log.w(TAG, "DataWedge not available on this device", it) }
   }
}
