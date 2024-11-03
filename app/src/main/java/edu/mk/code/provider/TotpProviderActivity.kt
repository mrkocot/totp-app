package edu.mk.code.provider

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import edu.mk.code.crypto.Cryptor
import edu.mk.code.persistence.Saviour
import edu.mk.code.prompts.BiometricExecutor
import edu.mk.code.R
import edu.mk.code.crypto.CodeGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * To use this data provider activity,
 * put extra int "totpId" and read extra string "totpCode" from the result.
 * Example way to use this activity from your activity:
 * ```
 * private fun startTotpProvider(totpId: Int) {
 *     val requestId = 1234
 *     val intent = Intent(context, TotpProviderActivity::class.java)
 *     intent.putExtra("totpId", totpId)
 *     startActivityForResult(intent, requestId)
 * }
 *
 * override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
 *     super.onActivityResult(requestCode, resultCode, data)
 *     val totp = data?.getStringExtra("totpCode")
 *     ...
 * }
 * ```
 */
class TotpProviderActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val totpId = intent.getIntExtra(TOTP_ID, -1)
        val returnIntent = Intent()
        CoroutineScope(Dispatchers.IO).launch {
            val code = getCode(totpId)
            returnIntent.putExtra(TOTP_CODE, code)
            withContext(Dispatchers.Main) {
                setResult(RESULT_OK, returnIntent)
                finish()
            }
        }
    }

    private suspend fun getCode(totpId: Int): String {
        val allRecords = Saviour.load(this@TotpProviderActivity)
        val record = allRecords.find { it.id == totpId } ?: return NOT_FOUND
        val biometricExecutor = BiometricExecutor(
            getString(R.string.external_authorise_title),
            getString(R.string.external_authorise_subtitle, record.name),
            getString(R.string.external_authorise_cancel),
        )
        return try {
            val decrypted =
                Cryptor(this).decryptRecord(biometricExecutor, record)
            CodeGenerator.generate(decrypted.key, decrypted.settings)
        } catch (_: BiometricExecutor.BiometricPromptRejected) {
            REJECTED
        }
    }

    companion object {
        const val TOTP_ID = "totpId"
        const val TOTP_CODE = "totpCode"
    }
}

private const val NOT_FOUND = "<id not found>"
private const val REJECTED = "<rejected>"
