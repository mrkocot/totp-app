package edu.mk.code.prompts

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import edu.mk.code.crypto.CryptoConsts
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.Cipher

class BiometricExecutor(
    private val promptTitle: String,
    private val promptSubtitle: String?,
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
        biometricPrompt.display(cipher)
        return String(future.await(), CryptoConsts.CHARSET)
    }

    @Throws(BiometricPromptRejected::class)
    suspend fun encrypt(
        fragmentActivity: FragmentActivity,
        cipher: Cipher,
        plain: String
    ): ByteArray {
        val future = CompletableDeferred<ByteArray>()
        val executor = ContextCompat.getMainExecutor(fragmentActivity)
        val callback = EncryptionCallback(future, cipher, plain.toByteArray(CryptoConsts.CHARSET))
        val biometricPrompt = BiometricPrompt(fragmentActivity, executor, callback)
        biometricPrompt.display(cipher)
        return future.await()
    }

    @Throws(BiometricPromptRejected::class)
    private suspend fun BiometricPrompt.display(cipher: Cipher) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(promptTitle)
            .setSubtitle(promptSubtitle)
            .setNegativeButtonText(promptNegativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        withContext(Dispatchers.Main) {
            this@display.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
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

    private class EncryptionCallback(
        private val future: CompletableDeferred<ByteArray>,
        private val cipher: Cipher,
        private val plain: ByteArray,
    ) : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            future.complete(cipher.doFinal(plain))
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            future.completeExceptionally(BiometricPromptRejected(errString.toString()))
        }
    }

    class BiometricPromptRejected(message: String) : Exception(message)
}
