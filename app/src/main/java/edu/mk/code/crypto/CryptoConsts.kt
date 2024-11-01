package edu.mk.code.crypto

import android.security.keystore.KeyProperties
import javax.crypto.spec.GCMParameterSpec

object CryptoConsts {
    const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    const val KEY_SIZE = 256
    const val FULL_ALGORITHM_STRING = "$ALGORITHM/$BLOCK_MODE/$PADDING"
    const val TWO_PURPOSES = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    const val KEY_ALIAS_PREFIX = "_totp_"

    val CHARSET = Charsets.UTF_8

    fun ivSpec(iv: ByteArray) = GCMParameterSpec(128, iv)
}