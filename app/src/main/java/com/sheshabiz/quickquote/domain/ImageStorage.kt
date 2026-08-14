package com.sheshabiz.quickquote.domain

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Copies a picked image into app-private storage so it survives even if the source Uri's grant expires. */
suspend fun copyLogoToInternalStorage(context: Context, uri: Uri): String? =
    copyImageToInternalStorage(context, uri, "logos", "logo")

suspend fun copyProductImageToInternalStorage(context: Context, uri: Uri): String? =
    copyImageToInternalStorage(context, uri, "products", "product")

private suspend fun copyImageToInternalStorage(context: Context, uri: Uri, subdir: String, prefix: String): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, subdir).apply { mkdirs() }
            val destFile = File(dir, "${prefix}_${System.currentTimeMillis()}.png")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            destFile.absolutePath
        }.getOrNull()
    }
