package edu.mk.code.crypto

import android.security.keystore.KeyGenParameterSpec
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Encrypts and decrypts data.
 * Plaintext is a string, and encrypted data is a byte array, where:
 * - The first byte is number N - length of IV (0 < N < 128)
 * - Next N bytes is the IV
 * - The rest is the encrypted content itself
 */
class Cryptor(private val fragmentActivity: FragmentActivity, totpId: Int) {
    private val keystoreAlias = CryptoConsts.KEY_ALIAS_PREFIX + totpId
    private val biometricExecutor = BiometricExecutor(
        "Authorise",
        "Additional authentication necessary to access the code",
        "Cancel",
    )

    fun encrypt(plain: String): ByteArray {
        val cipher = getKey(false).asEncryptCipher()
        val iv = cipher.iv
        val content = cipher.doFinal(plain.toByteArray())
        return byteArrayOf(iv.size.toByte()) + cipher.iv + content
    }

    suspend fun decrypt(encrypted: ByteArray): String {
        val ivLen = encrypted.first().toInt()
        val iv = encrypted.copyOfRange(1, ivLen + 1)
        val content = encrypted.copyOfRange(ivLen + 1, encrypted.size)
        val cipher = getKey(true).asDecryptCipher(iv)
        return biometricExecutor.decrypt(fragmentActivity, cipher, content)
    }

    private fun getKey(requireAuth: Boolean): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (!keyStore.containsAlias(keystoreAlias)) {
            val keyGenerator = KeyGenerator.getInstance(CryptoConsts.ALGORITHM, "AndroidKeyStore")
            keyGenerator.init(getKeySpec(requireAuth))
            keyGenerator.generateKey()
        }

        val keyEntry = keyStore.getEntry(keystoreAlias, null) as KeyStore.SecretKeyEntry
        return keyEntry.secretKey
    }

    private fun getKeySpec(requireAuth: Boolean): KeyGenParameterSpec =
        KeyGenParameterSpec.Builder(keystoreAlias, CryptoConsts.TWO_PURPOSES)
            .setBlockModes(CryptoConsts.BLOCK_MODE)
            .setEncryptionPaddings(CryptoConsts.PADDING)
            .setKeySize(CryptoConsts.KEY_SIZE)
            .setUserAuthenticationRequired(requireAuth)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
}

private fun SecretKey.asEncryptCipher(): Cipher =
    Cipher.getInstance(CryptoConsts.FULL_ALGORITHM_STRING).also { it.init(Cipher.ENCRYPT_MODE, this) }

private fun SecretKey.asDecryptCipher(iv: ByteArray): Cipher =
    Cipher.getInstance(CryptoConsts.FULL_ALGORITHM_STRING).also {
        it.init(Cipher.DECRYPT_MODE, this, CryptoConsts.ivSpec(iv))
    }
