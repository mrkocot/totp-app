package edu.mk.code.display

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.mk.code.crypto.SecretTotpKey
import edu.mk.code.display.TotpRecord.Companion.RecordList

data class TotpRecord(
    val id: Int,
    val name: String,
    val key: SecretTotpKey
) {
    @Composable
    fun RecordCell() {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("($id)")
                Text(name)
            }
            Button(onClick = {}, modifier = Modifier.weight(1f)) {
                Text("*** ***")
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Button(onClick = {}) {
                    Text("X")
                }
            }
        }
    }

    companion object {
        @Composable
        fun RecordList(records: List<TotpRecord>) {
            Column(modifier = Modifier.fillMaxSize()) {
                records.forEach {
                    it.RecordCell()
                    HorizontalDivider(color = Color.Black, thickness = 1.dp)
                }
            }
        }
    }
}

@Preview(name = "ThreeRecordCells")
@Composable
fun ThreeRecordCellsPreview() {
    val records = listOf(
        TotpRecord(1, "Google", SecretTotpKey.Companion("ABC")),
        TotpRecord(2, "Facebook", SecretTotpKey.Companion("PQR")),
        TotpRecord(3, "Apple", SecretTotpKey.Companion("123")),
    )

    RecordList(records)
}
