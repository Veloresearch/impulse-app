package com.velocity.impulse.data

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secrets in [prefs], sealed with an AES-GCM key that never leaves the Android Keystore. A value
 * still stored plain by an older version is sealed the first time it is read.
 */
class Secure(private val prefs: SharedPreferences) {
    fun get(key: String): String? {
        prefs.getString(sealedKey(key), null)?.let { return open(it) }
        val plain = prefs.getString(key, null) ?: return null
        seal(plain)?.let { prefs.edit().putString(sealedKey(key), it).remove(key).apply() }
        return plain
    }

    /** Writes [value] sealed into [e]; null removes it. */
    fun put(e: SharedPreferences.Editor, key: String, value: String?): SharedPreferences.Editor {
        if (value == null) return e.remove(sealedKey(key)).remove(key)
        // A phone whose Keystore fails keeps the value as before rather than losing the sign-in.
        val sealed = seal(value) ?: return e.putString(key, value).remove(sealedKey(key))
        return e.putString(sealedKey(key), sealed).remove(key)
    }

    private fun sealedKey(key: String) = "$key.sealed"

    private fun seal(plain: String): String? = runCatching {
        val c = Cipher.getInstance(CIPHER)
        c.init(Cipher.ENCRYPT_MODE, key())
        Base64.encodeToString(c.iv + c.doFinal(plain.toByteArray()), Base64.NO_WRAP)
    }.getOrNull()

    private fun open(sealed: String): String? = runCatching {
        val b = Base64.decode(sealed, Base64.NO_WRAP)
        val c = Cipher.getInstance(CIPHER)
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, b, 0, IV))
        String(c.doFinal(b, IV, b.size - IV))
    }.getOrNull()

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "impulse.secrets"
        const val CIPHER = "AES/GCM/NoPadding"
        const val IV = 12
    }
}
