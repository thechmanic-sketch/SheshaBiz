package com.sheshabiz.quickquote.ui.common

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.appContainer

/**
 * Small, right-aligned business logo shown in page headers. Observes the business profile
 * directly so every header gets it "for free" without threading the profile through every
 * screen/ViewModel. Renders nothing when no logo has been set.
 */
@Composable
fun HeaderLogo(size: Dp = 36.dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val profile by remember(context) {
        context.appContainer().businessRepository.observeProfile()
    }.collectAsState(initial = null)

    val logoUri = profile?.logoUri
    val bitmap = remember(logoUri) {
        logoUri?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}
