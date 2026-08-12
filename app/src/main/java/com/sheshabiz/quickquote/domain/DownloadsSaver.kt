package com.sheshabiz.quickquote.domain

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/** Copies a generated PDF into the device's public Downloads folder so it shows up in file managers. */
object DownloadsSaver {

    fun saveToDownloads(context: Context, sourceFile: File, displayName: String): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, sourceFile, displayName)
        } else {
            saveLegacy(sourceFile, displayName)
        }

    private fun saveViaMediaStore(context: Context, sourceFile: File, displayName: String): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        return runCatching {
            resolver.openOutputStream(uri)?.use { out -> sourceFile.inputStream().use { it.copyTo(out) } }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        }.getOrDefault(false)
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(sourceFile: File, displayName: String): Boolean = runCatching {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        sourceFile.copyTo(File(downloadsDir, displayName), overwrite = true)
        true
    }.getOrDefault(false)
}
