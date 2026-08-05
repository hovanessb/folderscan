package com.leonell.android.composefolderscanner.data.settings

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the app needs to reach the on-premises folder-tracking server. */
data class ServerCredentials(
   val baseUrl: String = DEFAULT_BASE_URL,
   val username: String = "",
   val password: String = "",
) {
   val isComplete: Boolean
      get() = baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()

   /** Ready-to-send `Authorization` header value, or null when credentials are unset. */
   fun basicAuthHeader(): String? {
      if (username.isBlank() && password.isBlank()) return null
      val token = Base64.encodeToString(
         "$username:$password".toByteArray(Charsets.UTF_8),
         Base64.NO_WRAP,
      )
      return "Basic $token"
   }

   /** Retrofit requires a trailing slash on its base URL. */
   fun normalizedBaseUrl(): String {
      val trimmed = baseUrl.trim().ifBlank { DEFAULT_BASE_URL }
      val withScheme =
         if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed
         else "http://$trimmed"
      return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
   }

   companion object {
      const val DEFAULT_BASE_URL = "http://ft.Cheesecake.org"
   }
}

/**
 * Single source of truth for the server address and the operator's credentials.
 *
 * Values are read once at construction and then served from memory, so the network layer
 * and the UI observe the same [StateFlow] without touching disk on every request.
 */
@Singleton
class SettingsRepository @Inject constructor(
   @ApplicationContext context: Context,
) {
   private val store = SecureStore(context)

   private val mutableCredentials = MutableStateFlow(
      ServerCredentials(
         baseUrl = store.getString(KEY_BASE_URL, ServerCredentials.DEFAULT_BASE_URL),
         username = store.getString(KEY_USERNAME),
         password = store.getString(KEY_PASSWORD),
      ),
   )

   val credentials: StateFlow<ServerCredentials> = mutableCredentials.asStateFlow()

   /** False when the device keystore was unusable and credentials are stored in the clear. */
   val isStorageEncrypted: Boolean get() = store.isEncrypted

   fun save(credentials: ServerCredentials) {
      store.putString(KEY_BASE_URL, credentials.baseUrl.trim())
      store.putString(KEY_USERNAME, credentials.username.trim())
      store.putString(KEY_PASSWORD, credentials.password)
      mutableCredentials.value = credentials.copy(
         baseUrl = credentials.baseUrl.trim(),
         username = credentials.username.trim(),
      )
   }

   fun clear() {
      store.clear()
      mutableCredentials.value = ServerCredentials()
   }

   private companion object {
      const val KEY_BASE_URL = "base_url"
      const val KEY_USERNAME = "username"
      const val KEY_PASSWORD = "password"
   }
}
