package edu.mk.code.crypto

import android.security.keystore.KeyGenParameterSpec
import android.util.Log
import androidx.fragment.app.FragmentActivity
import edu.mk.code.display.TotpRecord
import edu.mk.code.prompts.BiometricExecutor
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlin.text.toByteArray

/**
 * Encrypts and decrypts data.
 * Plaintext is a string, and encrypted data is a byte array, where:
 * - The first byte is number N - length of IV (0 < N < 128)
 * - Next N bytes is the IV
 * - The rest is the encrypted content itself
 */
class Cryptor(private val fragmentActivity: FragmentActivity?) {
    suspend fun encrypt(
        plain: String,
        biometricExecutor: BiometricExecutor?,
        keystoreAlias: String
    ): ByteArray {
        val key = getKey(keystoreAlias)
        val cipher = key.asEncryptCipher()
        val iv = cipher.iv
        val content = if (CryptoConsts.shouldKeyRequireAuth(keystoreAlias)) {
            fragmentActivity ?: error("Fragment activity is necessary for biometric authentication")
            biometricExecutor?.encrypt(fragmentActivity, cipher, plain)
                ?: error("Biometric executor is necessary for biometric authentication")
        } else {
            cipher.doFinal(plain.toByteArray(CryptoConsts.CHARSET))
        }
        return byteArrayOf(iv.size.toByte()) + cipher.iv + content
    }

    suspend fun encryptRecord(
        biometricExecutor: BiometricExecutor,
        plainRecord: TotpRecord.WithStringKey,
    ): TotpRecord {
        val keystoreAlias = CryptoConsts.KEY_ALIAS_SECRET + plainRecord.id
        val encryptedKey = encrypt(plainRecord.key, biometricExecutor, keystoreAlias)
        return plainRecord.withoutStringKey(encryptedKey)
    }

    suspend fun decrypt(
        encrypted: ByteArray,
        biometricExecutor: BiometricExecutor?,
        keystoreAlias: String,
    ): String {
        val ivLen = encrypted.first().toInt()
        val iv = encrypted.copyOfRange(1, ivLen + 1)
        val content = encrypted.copyOfRange(ivLen + 1, encrypted.size)
        val key = getKey(keystoreAlias)
        val cipher = key.asDecryptCipher(iv)
        return if (CryptoConsts.shouldKeyRequireAuth(keystoreAlias)) {
            fragmentActivity ?: error("Fragment activity is necessary for biometric authentication")
            biometricExecutor?.decrypt(fragmentActivity, cipher, content)
                ?: error("Biometric executor is necessary for biometric authentication")
        } else {
            String(cipher.doFinal(content), CryptoConsts.CHARSET)
        }
    }

    suspend fun decryptRecord(
        biometricExecutor: BiometricExecutor,
        encryptedRecord: TotpRecord,
    ): TotpRecord.WithStringKey {
        val keystoreAlias = CryptoConsts.KEY_ALIAS_SECRET + encryptedRecord.id
        val decryptedKey = decrypt(encryptedRecord.key, biometricExecutor, keystoreAlias)
        return encryptedRecord.withStringKey(decryptedKey)
    }

    private fun getKey(keystoreAlias: String): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (!keyStore.containsAlias(keystoreAlias)) {
            val keyGenerator = KeyGenerator.getInstance(CryptoConsts.ALGORITHM, "AndroidKeyStore")
            keyGenerator.init(getKeySpec(keystoreAlias))
            keyGenerator.generateKey()
        }

        val keyEntry = keyStore.getEntry(keystoreAlias, null) as KeyStore.SecretKeyEntry
        return keyEntry.secretKey
    }

    private fun getKeySpec(keystoreAlias: String): KeyGenParameterSpec =
        KeyGenParameterSpec.Builder(keystoreAlias, CryptoConsts.TWO_PURPOSES)
            .setBlockModes(CryptoConsts.BLOCK_MODE)
            .setEncryptionPaddings(CryptoConsts.PADDING)
            .setKeySize(CryptoConsts.KEY_SIZE)
            .setUserAuthenticationRequired(CryptoConsts.shouldKeyRequireAuth(keystoreAlias))
            .setInvalidatedByBiometricEnrollment(true)
            .build()

    companion object {
        fun deleteKey(record: TotpRecord) {
            val keystoreAlias = CryptoConsts.KEY_ALIAS_SECRET + record.id
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.deleteEntry(keystoreAlias)
        }
    }
}

private fun SecretKey.asEncryptCipher(): Cipher =
    Cipher.getInstance(CryptoConsts.FULL_ALGORITHM_STRING).also { it.init(Cipher.ENCRYPT_MODE, this) }

private fun SecretKey.asDecryptCipher(iv: ByteArray): Cipher =
    Cipher.getInstance(CryptoConsts.FULL_ALGORITHM_STRING).also {
        it.init(Cipher.DECRYPT_MODE, this, CryptoConsts.ivSpec(iv))
    }

//private fun SecretKey.isUserAuthenticationRequired(): Boolean =
//    SecretKeyFactory
//        .getInstance(this.algorithm, "AndroidKeyStore")
//        .getKeySpec(this, KeyGenParameterSpec::class.java)
//        .let { it as KeyGenParameterSpec }
//        .isUserAuthenticationRequired
