package com.android.codetest.biometric

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AndroidException
import java.nio.charset.Charset
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptographyUtils {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val YOUR_SECRET_KEY_NAME = "Y0UR$3CR3TK3YN@M3"
    private const val KEY_SIZE = 128
    private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES

    //region create Secret Key
    private fun getOrCreateSecretKey(keyName: String): SecretKey {
        //1
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)// Keystore must be loaded before it can be accessed
        keyStore.getKey(keyName, null)?.let {
            return it as SecretKey
        }
        //2
        val paramBuilder = KeyGenParameterSpec.Builder(
            keyName,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
        paramBuilder.apply {
            setBlockModes(ENCRYPTION_BLOCK_MODE)
            setEncryptionPaddings(ENCRYPTION_PADDING)
            setKeySize(KEY_SIZE)
            setUserAuthenticationRequired(true)
        }
        // 3
        val keyGenParams = paramBuilder.build()
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
        )
        keyGenerator.init(keyGenParams)
        return keyGenerator.generateKey()
    }
    //endregion

    //region get cipher instance
    private fun getCipher(): Cipher {
        val transformation = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"
        return Cipher.getInstance(transformation)
    }
    //endregion

    //region initialize cipher
    /**
     * SecretKey is generated only once — when you use it for the first time.
     * If cipher requires it later, it’ll use the same SecretKey, executing
     */
    fun getInitializedCipherForEncryption(): Cipher {
        val cipher = getCipher()
        val secretKey = getOrCreateSecretKey(YOUR_SECRET_KEY_NAME)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        return cipher
    }
    //endregion

    //region cipher decryption instance
    fun getCipherDecryptInstance(initialVector: ByteArray? = null): Cipher {
        val cipher = getCipher()
        val secretKey = getOrCreateSecretKey(YOUR_SECRET_KEY_NAME)
        cipher.init(
            Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(KEY_SIZE, initialVector)
        )
        return cipher

    }
    //endregion

    fun decryptData(ciphertext: ByteArray, cipher: Cipher): String {
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charset.forName("UTF-8"))
    }

    //region converts plaintext to ciphertext
    fun encryptData(plaintext: String, cipher: Cipher): EncryptedMessage {
        val ciphertext = cipher
            .doFinal(plaintext.toByteArray(Charset.forName("UTF-8")))
        return EncryptedMessage(ciphertext, cipher.iv, PREF_KEY)
    }
    //endregion


}