package edu.mk.code.persistence
import android.content.Context
import com.google.gson.JsonSyntaxException
import edu.mk.code.crypto.CryptoConsts
import edu.mk.code.display.TotpRecord
import edu.mk.code.R
import edu.mk.code.prompts.AlertableException
import org.apache.commons.codec.binary.Base64
import java.io.File

object Exporter {
    private val base64 = Base64()

    init {
        System.loadLibrary("exportEncryption")
    }

    fun export(
        context: Context,
        password: String,
        decryptedRecords:List<TotpRecord.WithStringKey>
    ) {
        if (password.isEmpty()) {
            throw ExportError(R.string.error_password_empty)
        } else if (decryptedRecords.isNotEmpty()) {
            val sd = context.getExternalFilesDir(null)!!
            val file = File(sd, CryptoConsts.EXPORT_FILE)
            val recordsJson = JsonOperator.dumpDecoded(decryptedRecords)
            file.writeBytes(encryptForExport(password, recordsJson))
        }
    }

    private fun encryptForExport(password: String, plain: String): ByteArray {
        val passwordBytes = password.toByteArray(CryptoConsts.CHARSET)
        val plainBytes = plain.toByteArray(CryptoConsts.CHARSET)
        val encryptedBytes = encrypt(passwordBytes, plainBytes)
        return base64.encodeAsString(encryptedBytes).toByteArray(ASCII)
    }

    fun import(context: Context, password: String): List<TotpRecord.WithStringKey> {
        if (password.isEmpty()) throw ImportError(R.string.error_password_empty)
        val sd = context.getExternalFilesDir(null)!!
        val file = File(sd, CryptoConsts.EXPORT_FILE)
        return if (file.exists()) {
            val plain = decryptFromImport(password, file.readBytes())
            try {
                JsonOperator.loadDecoded(plain.toString(CryptoConsts.CHARSET))
            } catch (_: JsonSyntaxException) {
                throw ImportError(R.string.error_import_wrong_password)
            }
        } else throw ImportError(R.string.error_export_file_not_found)
    }

    private fun decryptFromImport(password: String, encrypted: ByteArray): ByteArray {
        val passwordBytes = password.toByteArray()
        val encryptedBytes = base64.decode(String(encrypted, ASCII))
        return decrypt(passwordBytes, encryptedBytes)
    }

    @Suppress("unused")
    private external fun encrypt(password: ByteArray, plain: ByteArray): ByteArray

    @Suppress("unused")
    private external fun decrypt(password: ByteArray, encrypted: ByteArray): ByteArray

    class ExportError(override val messageId: Int) : AlertableException() {
        override val titleId: Int = R.string.export_failed
    }

    class ImportError(override val messageId: Int) : AlertableException() {
        override val titleId: Int = R.string.import_failed
    }
}

private val ASCII = Charsets.US_ASCII
