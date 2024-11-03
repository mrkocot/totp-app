package edu.mk.code.display

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.mk.code.crypto.CryptoConsts
import edu.mk.code.crypto.GeneratorSettings
import edu.mk.code.display.TotpRecord.Companion.RecordList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TotpRecord(
    val id: Int,
    val name: String,
    val key: ByteArray,
    val settings: GeneratorSettings = GeneratorSettings(),
) {
    @Composable
    fun RecordCell(
        codeGenerator: suspend (TotpRecord) -> String?,
        recordRemover: (TotpRecord) -> Unit,
    ) {
        var codeShown by remember { mutableStateOf(hiddenCode()) }
        var codeButtonEnabled by remember { mutableStateOf(true) }

        val contentSetter: (String, Boolean) -> Unit = { code, enabled ->
            codeShown = code
            codeButtonEnabled = enabled
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("($id)")
                Text(name)
            }
            Button(
                onClick = { showCode(codeGenerator, contentSetter) },
                modifier = Modifier.weight(1f),
                enabled = codeButtonEnabled
            ) {
                Text(codeShown.formatCodeForDisplay())
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Button(onClick = { recordRemover(this@TotpRecord) } ) {
                    Text("X")
                }
            }
        }
    }

    private fun showCode(
        codeGenerator: suspend (TotpRecord) -> String?,
        contentSetter: (String, Boolean) -> Unit,
    ) {
        contentSetter(loadingCode(), false)
        background.launch {
            val code = codeGenerator(this@TotpRecord)
            code?.let {
                contentSetter(it, false)
                delay(CryptoConsts.CODE_SHOW_TIME_SECONDS * 1_000L)
            }
            contentSetter(hiddenCode(), true)
        }
    }

    private fun hiddenCode(): String = HIDDEN_CODE.repeat(settings.digits)
    private fun loadingCode(): String = LOADING_CODE.repeat(settings.digits)

    private fun String.formatCodeForDisplay(): String {
        if (length != settings.digits) {
            Log.w("MK", "Expected the code to have ${settings.digits} digits, but got $length")
        }
        val builder = StringBuilder()
        this.forEachIndexed { index, digit ->
            if (index % 3 == 0 && index != 0) builder.append(" ")
            builder.append(digit)
        }
        return builder.toString()
    }

    fun withStringKey(textKey: String) =
        WithStringKey(id, name, textKey, settings)

    companion object {
        private val background = CoroutineScope(Dispatchers.Default)

        @Composable
        fun RecordList(
            records: SnapshotStateList<TotpRecord>,
            codeGenerator: suspend (TotpRecord) -> String?,
            recordRemover: (TotpRecord) -> Unit,
        ) {
            val stateRecords = remember { records }
            Column(modifier = Modifier.fillMaxSize()) {
                stateRecords.forEach {
                    it.RecordCell(codeGenerator, recordRemover)
                    HorizontalDivider(color = Color.Black, thickness = 1.dp)
                }
            }
        }
    }

    data class WithStringKey(
        val id: Int,
        val name: String,
        val key: String,
        val settings: GeneratorSettings,
    ) {
        fun withoutStringKey(byteArrayKey: ByteArray): TotpRecord =
            TotpRecord(id, name, byteArrayKey, settings)
    }
}

private const val HIDDEN_CODE = "*"
private const val LOADING_CODE = "_"

@Preview(name = "ThreeRecordCells")
@Composable
private fun ThreeRecordCellsPreview() {
    RecordList(exampleThreeRecords, ::fakeCodeGenerator) { exampleThreeRecords.remove(it) }
}

private val exampleThreeRecords =
    mutableStateListOf(
        TotpRecord(1, "Google", "ABC".toByteArray()),
        TotpRecord(2, "Facebook", "PQR".toByteArray(), GeneratorSettings(digits = 9)),
        TotpRecord(3, "Apple", "123".toByteArray(), GeneratorSettings(digits = 4)),
    )

private suspend fun fakeCodeGenerator(record: TotpRecord): String {
    delay(2000)
    return "12345678901234567890".take(record.settings.digits)
}
