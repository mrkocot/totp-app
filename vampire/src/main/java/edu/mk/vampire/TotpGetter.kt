package edu.mk.vampire

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import edu.mk.vampire.ui.theme.CodeManagerTheme
import kotlin.random.Random

class TotpGetter : ComponentActivity() {
    private val totpState = mutableStateOf("")
    private var totpValue by totpState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CodeManagerTheme {
                SimplestFormPossible()
            }
        }
    }

    @Composable
    fun SimplestFormPossible() {
        var code by remember { mutableIntStateOf(0) }
        var totp by remember { totpState }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TextField(
                label = { "TOTP id" },
                modifier = Modifier.fillMaxWidth(),
                value = code.toString(),
                onValueChange = {
                    val newValue = it.toIntOrNull()
                    if (newValue != null) {
                        code = newValue
                    } else if (it.isEmpty()) {
                        code = 0
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Button(
                onClick = {
                    startTotpProvider(code)
                }
            ) { Text("Get TOTP") }
            Text(totp)
        }
    }

    private fun startTotpProvider(totpId: Int) {
        val requestId = Random.nextInt()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setClassName(
                "edu.mk.code",
                "edu.mk.code.provider.TotpProviderActivity"
            )
            putExtra("totpId", totpId)
        }
        startActivityForResult(intent, requestId)
    }

    @Deprecated("Intentionally overrides deprecated onActivityResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val totp = data?.getStringExtra("totpCode")
        totpValue = "Returned code: $totp"
    }
}