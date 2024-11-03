package edu.mk.code.persistence

import android.content.Context
import edu.mk.code.crypto.CryptoConsts
import edu.mk.code.crypto.Cryptor
import edu.mk.code.display.TotpRecord
import java.io.File

object Saviour {
    suspend fun save(context: Context, records: List<TotpRecord>) {
        val storage = context.filesDir
        val file = File(storage, CryptoConsts.SAVE_FILE)
        val json = JsonOperator.dumpEncoded(records)
        savePlainForDebug(storage, json)
        val encrypted = Cryptor(null).encrypt(json, null, CryptoConsts.KEY_ALIAS_MAIN)
        file.writeBytes(encrypted)
    }

    suspend fun load(context: Context): List<TotpRecord> {
        val storage = context.filesDir
        val file = File(storage, CryptoConsts.SAVE_FILE)
        return if (file.exists()) {
            val encrypted = file.readBytes()
            val json = Cryptor(null).decrypt(encrypted, null, CryptoConsts.KEY_ALIAS_MAIN)
            JsonOperator.loadEncoded(json)
        } else emptyList()
    }

}

private fun savePlainForDebug(folder: File, json: String) {
    val file = File(folder, "debugTotp.txt")
    file.writeText(json, CryptoConsts.CHARSET)
}
