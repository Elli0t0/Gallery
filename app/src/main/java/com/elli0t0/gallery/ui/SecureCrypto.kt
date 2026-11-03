package com.elli0t0.gallery.ui

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// AES-256/GCM files in app-private storage, key held in AndroidKeyStore.
// File layout: 12-byte IV followed by ciphertext.
object SecureCrypto {
    private const val ALIAS = "gallery_secure"

    fun secureDir(context: Context): File =
        File(context.filesDir, "secure").apply { if (!exists()) mkdirs() }

    fun hashPin(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun encrypt(context: Context, plain: ByteArray): File {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val ciphertext = cipher.doFinal(plain)
        val file = File(secureDir(context), "${System.currentTimeMillis()}_${plain.size}.enc")
        file.outputStream().use {
            it.write(cipher.iv)
            it.write(ciphertext)
        }
        return file
    }

    fun decrypt(context: Context, encName: String): ByteArray {
        val bytes = File(secureDir(context), encName).readBytes()
        require(bytes.size > 12)
        val iv = bytes.copyOfRange(0, 12)
        val ciphertext = bytes.copyOfRange(12, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    fun delete(context: Context, encName: String) {
        File(secureDir(context), encName).delete()
    }
}
