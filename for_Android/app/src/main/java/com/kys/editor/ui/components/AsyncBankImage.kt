package com.kys.editor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kys.editor.codec.ImageBank
import com.kys.editor.util.toSafeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Asynchronously loads and displays a bitmap from an ImageBank.
 * Shows a placeholder while loading, caches result globally.
 *
 * @param bank The image bank
 * @param index Image index
 * @param maxDim If > 0, the decoded bitmap will be subsampled to fit this dimension (thumbnails)
 */
@Composable
fun AsyncBankImage(
    bank: ImageBank,
    index: Int,
    modifier: Modifier = Modifier,
    maxDim: Int = 0,
    placeholderColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    var bitmap by remember(bank, index, maxDim) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(bank, index, maxDim) {
        bitmap = withContext(Dispatchers.IO) {
            try { bank.getBitmapAsync(index, maxDim) } catch (_: Exception) { null }
        }
    }

    Box(modifier.background(placeholderColor), contentAlignment = Alignment.Center) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.toSafeImageBitmap(),
                contentDescription = "#$index",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
