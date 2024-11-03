package edu.mk.code.crypto

import android.security.keystore.KeyProperties
import javax.crypto.spec.GCMParameterSpec

object CryptoConsts {
    const val TOTP_ALGORITHM = "HmacSHA1"

    const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    const val KEY_SIZE = 256
    const val FULL_ALGORITHM_STRING = "$ALGORITHM/$BLOCK_MODE/$PADDING"
    const val TWO_PURPOSES = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT

    const val BIOMETRIC_SEPARATION_MILLIS = 500L

    const val KEY_ALIAS_MAIN = "_totp_"
    const val KEY_ALIAS_SECRET = "_totp_key_"

    const val SAVE_FILE = "totp.bin"
    const val EXPORT_FILE = "export.txt"

    val CHARSET = Charsets.UTF_8

    const val CODE_SHOW_TIME_SECONDS = 5

    fun ivSpec(iv: ByteArray) = GCMParameterSpec(128, iv)

    fun shouldKeyRequireAuth(keystoreAlias: String): Boolean =
        keystoreAlias != KEY_ALIAS_MAIN
}