package edu.mk.code.persistence

import com.google.gson.Gson
import edu.mk.code.display.TotpRecord
import org.apache.commons.codec.binary.Base64

object JsonOperator {
    fun dumpDecoded(records: List<TotpRecord.WithStringKey>): String =
        Gson().toJson(records)

    fun dumpEncoded(records: List<TotpRecord>): String {
        val base64Records = records.map {
            it.withStringKey(Base64().encodeAsString(it.key))
        }
        return dumpDecoded(base64Records)
    }

    fun loadDecoded(json: String): List<TotpRecord.WithStringKey> =
        Gson().fromJson(json, Array<TotpRecord.WithStringKey>::class.java).toList()

    fun loadEncoded(json: String): List<TotpRecord> {
        val base64Records = loadDecoded(json)
        return base64Records.map {
            it.withoutStringKey(Base64().decode(it.key))
        }
    }
}
