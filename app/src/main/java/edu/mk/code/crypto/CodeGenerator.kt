package edu.mk.code.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base32
import java.nio.ByteBuffer

object CodeGenerator {
    @OptIn(ExperimentalStdlibApi::class)
    fun generate(key: String, settings: GeneratorSettings = GeneratorSettings()): String {
        val keySpec = SecretKeySpec(key.fromBase32(), CryptoConsts.TOTP_ALGORITHM)
        val hmac = Mac.getInstance(CryptoConsts.TOTP_ALGORITHM).also { it.init(keySpec) }

        val timeSteps = (System.currentTimeMillis() - settings.beginning) / settings.millis
        val result = hmac.doFinal(timeSteps.toByteArray())

        return getDigits(result, settings.digits)
    }

    fun getDigits(result: ByteArray, digitNumber: Int): String {
        val offset = result.last().toInt() and 0xF
        val result31 = result.copyOfRange(offset, offset + 4).toUInt() and 0x7FFFFFFFu
        return result31.toString().takeLast(digitNumber).padStart(digitNumber, '0')
    }
}

private fun String.fromBase32(): ByteArray =
    Base32().decode(this)

/** Time-to-byte-array conversion, as per RFC 6238 */
private fun Long.toByteArray(): ByteArray =
    ByteBuffer.allocate(8).putLong(this).array()

private fun ByteArray.toUInt(): UInt =
    ByteBuffer.wrap(this).int.toUInt()
