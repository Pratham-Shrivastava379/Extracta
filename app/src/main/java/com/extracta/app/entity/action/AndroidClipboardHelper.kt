package com.extracta.app.entity.action

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper

class AndroidClipboardHelper(context: Context) {
    private val appContext = context.applicationContext

    fun copy(value: String) {
        val copyToClipboard = Runnable {
            val clipboard = appContext.getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText("Detected detail", value))
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            copyToClipboard.run()
        } else {
            Handler(Looper.getMainLooper()).post(copyToClipboard)
        }
    }
}
