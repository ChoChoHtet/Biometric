package com.android.codetest.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

object BiometricUtils {
    private fun hasBiometricCapability(context: Context): Int {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BIOMETRIC_STRONG)
    }

    fun isBiometricReady(context: Context): Boolean {
        return hasBiometricCapability(context) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: MainActivity, lister: BiometricAuthListener,
        cryptoObject: BiometricPrompt.CryptoObject? = null
    ) {
        // Prepare BiometricPrompt Dialog
        val promptInfo = createBiometricBuilder()
        val biometricPrompt = biometricPromptCallback(activity, lister)

        // Authenticate with a CryptoObject if provided, otherwise default authentication
        biometricPrompt.apply {
            if (cryptoObject == null) authenticate(promptInfo) else authenticate(
                promptInfo,
                cryptoObject
            )
        }

    }

    private fun createBiometricBuilder(): BiometricPrompt.PromptInfo {
        val builderInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setDescription("Log in using your biometric credential")
            .setNegativeButtonText("Cancel")
        builderInfo.apply {

        }
        return builderInfo.build()
    }

    private fun biometricPromptCallback(
        activity: MainActivity, lister: BiometricAuthListener
    ): BiometricPrompt {
        // Attach calling Activity
        val executor = ContextCompat.getMainExecutor(activity)
        // Attach callback handlers
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                lister.onBiometricAuthenticationError(errorCode, errString.toString())
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                lister.onBiometricAuthenticationSuccess(result)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                lister.onBiometricAuthenticationFail()
            }
        }
        return BiometricPrompt(activity, executor, callback)

    }
}