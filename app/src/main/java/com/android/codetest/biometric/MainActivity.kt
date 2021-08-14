package com.android.codetest.biometric

import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.text.TextUtilsCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import javax.crypto.Cipher

const val PREF_KEY="biometric_share_key"
class MainActivity : AppCompatActivity(), BiometricAuthListener {
    private val ciphertextWrapper
        get() = PreferenceUtil.getEncryptedMessage(this, PREF_KEY)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnLogin.setOnClickListener {
            login()
        }
        if (BiometricUtils.isBiometricReady(this)) {
            btFingerPrint.setOnClickListener {
                if (ciphertextWrapper != null ){
                   showBiometricPromptToDecrypt()
                }
            }
        }

    }

    private fun login() {
        if (BiometricUtils.isBiometricReady(this)) {
            showBiometricPromptForEncrypt()
        }

    }

    private fun showBiometricPromptForEncrypt() {
        // Create Cryptography Object
        val cryptoObject = BiometricPrompt.CryptoObject(
            CryptographyUtils.getInitializedCipherForEncryption()
        )
        //show BiometricPrompt
        BiometricUtils.showBiometricPrompt(this, this, cryptoObject)
    }

    private fun showBiometricPromptToDecrypt() {
        ciphertextWrapper?.initializationVector?.let { it ->
            // Retrieve Cryptography Object
            val cryptoObject = BiometricPrompt.CryptoObject(
                CryptographyUtils.getCipherDecryptInstance(it)
            )
            // Show BiometricPrompt With Cryptography Object
            BiometricUtils.showBiometricPrompt(
                activity = this,
                this,
                cryptoObject = cryptoObject
            )
        }
    }

    private fun encryptMessage(cipher: Cipher) {
        val accessToken = UUID.randomUUID().toString()
        val encryptedMessage = CryptographyUtils.encryptData(accessToken, cipher)
        PreferenceUtil.storeEncryptedMessage(
            this,
            prefKey = encryptedMessage.prefKey, encryptedMessage
        )
        Toast.makeText(applicationContext, "Your secret is saved.", Toast.LENGTH_SHORT).show()
    }
    private fun decryptAndDisplay(cipher: Cipher) {
        ciphertextWrapper?.cipherText?.let { it ->
            val decryptedMessage = CryptographyUtils.decryptData(it, cipher)
            val intent = Intent(this,DetailActivity::class.java)
            intent.putExtra("token",decryptedMessage)
            startActivity(intent)
        }
    }

    override fun onBiometricAuthenticationSuccess(result: BiometricPrompt.AuthenticationResult) {
        Toast.makeText(applicationContext, "Authentication succeeded!", Toast.LENGTH_SHORT).show()
        if (ciphertextWrapper == null){
            val username = editTextPhone.text.toString()
            val password = editTextTextPassword.text.toString()
            result.cryptoObject?.cipher?.let {
                if (!TextUtils.isEmpty(username) and !TextUtils.isEmpty(password)) {
                    encryptMessage(it)
                }
            }
        }else {
            result.cryptoObject?.cipher?.let {
                decryptAndDisplay(it)
            }
        }

    }

    override fun onBiometricAuthenticationError(errorCode: Int, errorMessage: String) {
        Toast.makeText(
            applicationContext,
            "Authentication error: $errorMessage",
            Toast.LENGTH_SHORT
        )
            .show()
    }

    override fun onBiometricAuthenticationFail() {
        Toast.makeText(
            applicationContext, "Authentication failed",
            Toast.LENGTH_SHORT
        )
            .show()
    }
}