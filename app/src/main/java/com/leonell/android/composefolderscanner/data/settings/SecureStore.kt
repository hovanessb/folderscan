package com.leonell.android.composefolderscanner.data.settings

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small AES-GCM wrapper over [SharedPreferences] whose key lives in the hardware-backed
 * AndroidKeyStore, so scanner credentials are never written to disk in the clear.
 *
 * This intentionally uses only platform APIs (available since API 23) rather than
 * `androidx.security:security-crypto`, which pins the app to an alpha artifact and is
 * known to fail hard when the device keystore is reset.
 *
 * If the keystore is unavailable the store degrades to reading/writing plaintext rather
 * than crashing the app; [isEncrypted] reports which mode is in effect so the UI can warn.
 */
class SecureStore(context: Context, fileName: String = PREFS_FILE) {

   private val prefs: SharedPreferences =
      context.applicationContext.getSharedPreferences(fileName, Context.MODE_PRIVATE)

   private val secretKey: SecretKey? = runCatching { loadOrCreateKey() }
      .onFailure { Log.e(TAG, "Keystore unavailable, falling back to plaintext", it) }
      .getOrNull()

   /** False when the device keystore could not be used and values are stored in the clear. */
   val isEncrypted: Boolean get() = secretKey != null

   fun getString(key: String, default: String = ""): String {
      val stored = prefs.getString(key, null) ?: return default
      val plain = secretKey?.let { decrypt(stored, it) } ?: stored
      return plain ?: default
   }

   fun putString(key: String, value: String) {
      val stored = secretKey?.let { encrypt(value, it) } ?: value
      prefs.edit().putString(key, stored).apply()
   }

   fun clear() = prefs.edit().clear().apply()

   private fun encrypt(plain: String, key: SecretKey): String? = runCatching {
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.ENCRYPT_MODE, key)
      val iv = cipher.iv
      val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
      // Prefix the payload with the 12-byte GCM nonce so each write is self-describing.
      Base64.encodeToString(iv + body, Base64.NO_WRAP)
   }.onFailure { Log.e(TAG, "Encryption failed", it) }.getOrNull()

   private fun decrypt(stored: String, key: SecretKey): String? = runCatching {
      val raw = Base64.decode(stored, Base64.NO_WRAP)
      if (raw.size <= GCM_IV_LENGTH) return@runCatching null
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(
         Cipher.DECRYPT_MODE,
         key,
         GCMParameterSpec(GCM_TAG_BITS, raw, 0, GCM_IV_LENGTH),
      )
      String(cipher.doFinal(raw, GCM_IV_LENGTH, raw.size - GCM_IV_LENGTH), Charsets.UTF_8)
   }.onFailure { Log.e(TAG, "Decryption failed; clearing stale value", it) }.getOrNull()

   private fun loadOrCreateKey(): SecretKey {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
      (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

      val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
      generator.init(
         KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
         )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build(),
      )
      return generator.generateKey()
   }

   private companion object {
      const val TAG = "SecureStore"
      const val PREFS_FILE = "folder_scanner_credentials"
      const val ANDROID_KEYSTORE = "AndroidKeyStore"
      const val KEY_ALIAS = "folder_scanner_credentials_key"
      const val TRANSFORMATION = "AES/GCM/NoPadding"
      const val GCM_IV_LENGTH = 12
      const val GCM_TAG_BITS = 128
   }
}
