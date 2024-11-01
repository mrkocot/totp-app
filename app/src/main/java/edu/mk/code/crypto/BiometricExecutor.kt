package edu.mk.code.crypto

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.Cipher

class BiometricExecutor(
    private val promptTitle: String,
    private val promptSubtitle: String,
    private val promptNegativeButtonText: String,
) {
    @Throws(BiometricPromptRejected::class)
    suspend fun decrypt(
        fragmentActivity: FragmentActivity,
        cipher: Cipher,
        encrypted: ByteArray
    ): String {
        val future = CompletableDeferred<ByteArray>()
        val executor = ContextCompat.getMainExecutor(fragmentActivity)
        val callback = DecryptionCallback(future, cipher, encrypted)
        val biometricPrompt = BiometricPrompt(fragmentActivity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(promptTitle)
            .setSubtitle(promptSubtitle)
            .setNegativeButtonText(promptNegativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        withContext(Dispatchers.Main) {
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
        return String(future.await(), CryptoConsts.CHARSET)
    }

    private class DecryptionCallback(
        private val future: CompletableDeferred<ByteArray>,
        private val cipher: Cipher,
        private val encrypted: ByteArray,
    ) : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            future.complete(cipher.doFinal(encrypted))
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            future.completeExceptionally(BiometricPromptRejected(errString.toString()))
        }
    }

    class BiometricPromptRejected(message: String) : Exception(message)
}
