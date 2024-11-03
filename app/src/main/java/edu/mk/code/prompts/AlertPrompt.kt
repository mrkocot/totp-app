package edu.mk.code.prompts

import android.app.AlertDialog
import android.content.Context
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AlertPrompt(private val context: Context) {
    fun ask(
        title: String,
        message: String,
        onOk: () -> Unit,
        onCancel: () -> Unit = {},
        okText: Int = android.R.string.ok,
        cancelText: Int = android.R.string.cancel,
    ) {
        Looper.myLooper() ?: Looper.prepare()
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(okText) { _, _ -> onOk() }
            .setNegativeButton(cancelText) { _, _ -> onCancel() }
            .show()
    }

    suspend fun inform(title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .showOnMainThread()
    }

    suspend fun inform(title: Int, message: Int) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .showOnMainThread()
    }

    suspend fun error(e: AlertableException) {
        inform(e.titleId, e.messageId)
    }

    private suspend fun AlertDialog.Builder.showOnMainThread() {
        withContext(Dispatchers.Main) {
            Looper.myLooper() ?: Looper.prepare()
            this@showOnMainThread.show()
        }
    }
}
