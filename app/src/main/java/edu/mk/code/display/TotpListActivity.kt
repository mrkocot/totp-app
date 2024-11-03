package edu.mk.code.display

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import edu.mk.code.R
import edu.mk.code.crypto.CodeGenerator
import edu.mk.code.crypto.CryptoConsts
import edu.mk.code.crypto.Cryptor
import edu.mk.code.crypto.GeneratorSettings
import edu.mk.code.persistence.Exporter
import edu.mk.code.persistence.Saviour
import edu.mk.code.prompts.AlertPrompt
import edu.mk.code.prompts.BiometricExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TotpListActivity : FragmentActivity() {
    private val records = mutableStateListOf<TotpRecord>()
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val alertGenerator = AlertPrompt(this)

    // BEWARE - the two-argument onCreate (below) is not called, override the one-argument version
    //   override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        records.clear()
        ioScope.launch {
            records.addAll(Saviour.load(this@TotpListActivity))
        }

        setContent {
            MainNav()
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
        ioScope.launch {
            Saviour.save(this@TotpListActivity, records)
        }
    }

    @Composable
    private fun MainNav() {
        val navController = rememberNavController()

        NavHost(navController, startDestination = "list") {
            composable("list") {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row {
                        Button(onClick = { navController.navigate("exportImport") }) {
                            Text(getString(R.string.export_import_button))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(onClick = { navController.navigate("create") }) {
                            Text(getString(R.string.totp_create_button))
                        }
                    }
                    HorizontalDivider(color = Color.Black, thickness = 4.dp)
                    TotpRecord.RecordList(records, ::generateCode, ::removeRecord)
                }
            }
            composable("exportImport") {
                ExportImport()
            }
            composable("create") {
                CreateTOTP(navController)
            }
        }
    }

    private suspend fun generateCode(record: TotpRecord): String? {
        val biometricExecutor = BiometricExecutor(
            getString(R.string.code_generate_authorise_title),
            getString(R.string.code_generate_authorise_subtitle, record.name, CryptoConsts.CODE_SHOW_TIME_SECONDS),
            getString(R.string.code_generate_authorise_cancel),
        )
        return try {
            val plainKey =
                Cryptor(this).decryptRecord(biometricExecutor, record).key
            CodeGenerator.generate(plainKey, record.settings)
        } catch (_: BiometricExecutor.BiometricPromptRejected) {
            null
        }
    }

    private fun removeRecord(record: TotpRecord) {
        Cryptor.deleteKey(record)
        records.remove(record)
    }

    @Composable
    private fun CreateTOTP(navController: NavController) {
        var name by remember { mutableStateOf("") }
        var digits by remember { mutableLongStateOf(6L) }
        var seconds by remember { mutableLongStateOf(30L) }
        var beginning by remember { mutableLongStateOf(0L) }
        var key by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(getString(R.string.totp_create_name)) },
                modifier = Modifier.fillMaxWidth(),
                isError = name.isBlank(),
                supportingText = {
                    if (name.isBlank()) {
                        Text(getString(R.string.totp_create_name_empty))
                    }
                },
            )
            LimitedNumberField(
                value = digits,
                onValueChange = { digits = it },
                label = getString(R.string.totp_create_digits),
                min = 1,
                max = 14,
            )
            LimitedNumberField(
                value = seconds,
                onValueChange = { seconds = it },
                label = getString(R.string.totp_create_seconds),
                min = 1,
                max = 60,
            )
            LimitedNumberField(
                value = beginning,
                onValueChange = { beginning = it },
                label = getString(R.string.totp_create_beginning),
                min = 0,
                max = System.currentTimeMillis() / 1000,
                overrideErrorMessage = getString(R.string.totp_create_beginning_too_big),
            )
            OutlinedTextField(
                value = key,
                onValueChange = { if (it.uppercase().isValidBase32()) key = it.uppercase() },
                label = { Text(getString(R.string.totp_create_key)) },
                modifier = Modifier.fillMaxWidth(),
                isError = key.isBlank() || !key.isValidBase32(),
                supportingText = {
                    if (key.isBlank() || !key.isValidBase32()) {
                        Text(getString(R.string.totp_create_key_invalid))
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            Button(
                onClick = {
                    val biometricExecutor = BiometricExecutor(
                        getString(R.string.totp_create_authorise_title),
                        getString(R.string.totp_create_authorise_subtitle, name),
                        getString(R.string.totp_create_authorise_cancel),
                    )
                    val plain = TotpRecord.WithStringKey(
                        id = records.map { it.id }.findFirstFree(),
                        name = name,
                        key = key,
                        settings = GeneratorSettings(
                            digits = digits.toInt(),
                            seconds = seconds.toInt(),
                            beginning = beginning,
                        )
                    )
                    ioScope.launch {
                        val encrypted = Cryptor(this@TotpListActivity).encryptRecord(
                            biometricExecutor,
                            plain,
                        )
                        records.add(encrypted)
                        withContext(Dispatchers.Main) {
                            navController.popBackStack()
                        }
                    }
                },
                enabled = name.isNotBlank()
                        && digits in 1..14
                        && seconds in 1..60
                        && beginning < System.currentTimeMillis() / 1000
                        && key.isNotBlank()
                        && key.isValidBase32(),
            ) {
                Text(getString(R.string.totp_create_submit))
            }
        }
    }

    @Composable
    private fun LimitedNumberField(
        value: Long,
        onValueChange: (Long) -> Unit,
        label: String,
        min: Long,
        max: Long,
        overrideErrorMessage: String? = null,
    ) {
        OutlinedTextField(
            label = { label },
            modifier = Modifier.fillMaxWidth(),
            value = value.toString(),
            onValueChange = {
                val newValue = it.toLongOrNull()
                if (newValue != null) {
                    onValueChange(newValue)
                } else if (it.isEmpty()) {
                    onValueChange(0)
                }
            },
            isError = value !in min..max,
            supportingText = {
                if (value !in min..max) {
                    Text(overrideErrorMessage ?: getString(R.string.value_bounds, label, min, max))
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }

    @Composable
    private fun ExportImport() {
        var password by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(getString(R.string.password)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Button(
                    onClick = { export(password); password = "" },
                    enabled = password.isNotEmpty()
                ) {
                    Text(getString(R.string.export_button))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { import(password); password = "" },
                    enabled = password.isNotEmpty()
                ) {
                    Text(getString(R.string.import_button))
                }
            }
        }
    }

    private fun export(password: String) {
        alertGenerator.ask(
            title = getString(R.string.export_confirm_title),
            message = resources.getQuantityString(R.plurals.export_confirm_message, records.size, records.size),
            onOk = { ioScope.launch { doExport(password) } },
            okText = R.string.export_button
        )
    }

    private suspend fun doExport(password: String) {
        try {
            val plainRecords = records.toList().mapNotNull {
                val biometricExecutor = BiometricExecutor(
                    getString(R.string.export_authorise_title, it.name),
                    null,
                    getString(R.string.export_skip_key)
                )
                delay(CryptoConsts.BIOMETRIC_SEPARATION_MILLIS)
                try {
                    Cryptor(this).decryptRecord(biometricExecutor, it)
                } catch (_: BiometricExecutor.BiometricPromptRejected) {
                    null
                }
            }
            Exporter.export(this@TotpListActivity, password, plainRecords)
            alertGenerator.inform(
                getString(R.string.export_finish_title),
                resources.getQuantityString(R.plurals.export_finish_message, plainRecords.size, plainRecords.size),
            )
        } catch (e: Exporter.ExportError) {
            alertGenerator.error(e)
        }
    }

    private fun import(password: String) {
        alertGenerator.ask(
            title = getString(R.string.import_confirm_title),
            message = resources.getQuantityString(R.plurals.import_confirm_message, records.size, records.size),
            onOk = { ioScope.launch { doImport(password) } },
            okText = R.string.import_button
        )
    }

    private suspend fun doImport(password: String) {
        try {
            val plainRecords = Exporter.import(this, password)
            val encryptedRecords = plainRecords.mapNotNull {
                val biometricExecutor = BiometricExecutor(
                    getString(R.string.import_authorise_title, it.name),
                    null,
                    getString(R.string.import_skip_key)
                )
                delay(CryptoConsts.BIOMETRIC_SEPARATION_MILLIS)
                try {
                    Cryptor(this).encryptRecord(biometricExecutor, it)
                } catch (_: BiometricExecutor.BiometricPromptRejected) {
                    null
                }
            }
            records.clear()
            records.addAll(encryptedRecords)
            alertGenerator.inform(
                getString(R.string.import_finish_title),
                resources.getQuantityString(R.plurals.import_finish_message, encryptedRecords.size, encryptedRecords.size),
            )
        } catch (e: Exporter.ImportError) {
            alertGenerator.error(e)
        }
    }
}

private fun List<Int>.findFirstFree(): Int {
    var i: Int = 1
    while (i in this) {
        if (i >= Int.MAX_VALUE) error("How did you make over 2 billion records?")
        i += 1
    }
    return i
}

private fun String.isValidBase32(): Boolean =
    this.all { it.isUpperCase() || it == '=' || it.digitToIntOrNull() in 2..7 }
