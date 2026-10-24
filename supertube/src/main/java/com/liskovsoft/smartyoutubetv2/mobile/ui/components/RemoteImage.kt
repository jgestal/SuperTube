package com.liskovsoft.smartyoutubetv2.mobile.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Minimal Compose image loader on top of Glide (already used by the 'common' module).
 * Glide handles memory/disk caching and bitmap pooling.
 */
@Composable
fun RemoteImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    width: Int = 640,
    height: Int = 360,
) {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<ImageBitmap?>(initialValue = null, url) {
        value = null
        if (url.isNullOrEmpty()) return@produceState
        val target = Glide.with(context).asBitmap().load(url).submit(width, height)
        try {
            val result: Bitmap? = withContext(Dispatchers.IO) {
                try { target.get() } catch (e: Exception) { null }
            }
            value = result?.asImageBitmap()
        } finally {
            if (value == null) Glide.with(context).clear(target)
        }
    }

    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        Crossfade(targetState = bitmap, label = "image") { image ->
            if (image != null) {
                Image(bitmap = image, contentDescription = null, contentScale = contentScale, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
