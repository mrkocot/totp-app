package edu.mk.code.crypto

sealed interface SecretTotpKey {
    suspend fun decrypt(cryptor: Cryptor): Plain
    suspend fun encrypt(cryptor: Cryptor): Encrypted

    companion object {
        operator fun invoke(base32: String): Plain = Plain(base32)
        operator fun invoke(encrypted: ByteArray): Encrypted = Encrypted(encrypted)
    }

    data class Plain(val base32: String) : SecretTotpKey {
        override suspend fun encrypt(cryptor: Cryptor): Encrypted = Encrypted(cryptor.encrypt(base32))
        override suspend fun decrypt(cryptor: Cryptor): Plain = this
    }

    data class Encrypted(val encrypted: ByteArray) : SecretTotpKey {
        override suspend fun encrypt(cryptor: Cryptor): Encrypted = this
        override suspend fun decrypt(cryptor: Cryptor): Plain = Plain(cryptor.decrypt(encrypted))
    }
}
