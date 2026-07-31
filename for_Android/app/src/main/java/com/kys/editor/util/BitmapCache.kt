package com.kys.editor.util

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Global LRU bitmap cache keyed by a string key.
 * Sized at 1/8 of available memory, capped.
 */
object BitmapCache {
    private var cache: android.util.LruCache<String, Bitmap>? = null
    private val lock = Any()
    private val decodeMutex = Mutex()

    fun init(ctx: Context) {
        synchronized(lock) {
            if (cache != null) return
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val maxBytes = am.memoryClass * 1024 * 1024 / 8
            cache = object : android.util.LruCache<String, Bitmap>(maxBytes) {
                override fun sizeOf(key: String, value: Bitmap): Int {
                    return value.allocationByteCount
                }
                override fun entryRemoved(
                    evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?
                ) {
                    if (evicted && !oldValue.isRecycled) {
                        oldValue.recycle()
                    }
                }
            }
        }
    }

    fun get(key: String): Bitmap? = cache?.get(key)

    fun put(key: String, bitmap: Bitmap) {
        cache?.put(key, bitmap)
    }

    fun clear() {
        synchronized(lock) {
            cache?.evictAll()
        }
    }

    /**
     * Decode a PNG byte array into a Bitmap, scaled down to fit maxDim if needed.
     * Uses cache. Must be called on a background thread.
     */
    fun decodePng(key: String, pngBytes: ByteArray, maxDim: Int = 0): Bitmap? {
        get(key)?.let { return it }
        val bmp = if (maxDim > 0) {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size, opts)
            var sample = 1
            while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) {
                sample *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size, decodeOpts)
        } else {
            BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)
        } ?: return null
        put(key, bmp)
        return bmp
    }

    suspend fun getOrDecode(
        key: String,
        maxDim: Int = 0,
        loader: suspend () -> ByteArray?
    ): Bitmap? = withContext(Dispatchers.IO) {
        get(key)?.let { return@withContext it }
        decodeMutex.withLock {
            get(key)?.let { return@withLock it }
            val bytes = loader() ?: return@withLock null
            decodePng(key, bytes, maxDim)
        }
    }
}

/** Convert a Bitmap to ImageBitmap, creating a defensive copy if needed for Compose. */
fun Bitmap.toSafeImageBitmap(): ImageBitmap {
    // Compose can use hardware bitmaps on API 26+, but for safety use ARGB_8888 copy
    return if (config == Bitmap.Config.ARGB_8888 && !isRecycled) {
        asImageBitmap()
    } else {
        copy(Bitmap.Config.ARGB_8888, false)?.asImageBitmap()
            ?: asImageBitmap()
    }
}
