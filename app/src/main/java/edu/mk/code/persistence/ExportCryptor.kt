package edu.mk.code.persistence

object ExportCryptor {
    init {
        System.loadLibrary("exportEncryption")
    }

    external fun encrypt(password: ByteArray, plain: ByteArray): ByteArray
    external fun decrypt(password: ByteArray, encrypted: ByteArray): ByteArray
}
