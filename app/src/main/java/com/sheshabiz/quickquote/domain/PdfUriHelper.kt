package com.sheshabiz.quickquote.domain

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object PdfUriHelper {
    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
