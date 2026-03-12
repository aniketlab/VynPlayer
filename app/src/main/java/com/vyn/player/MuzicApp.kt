package com.vyn.player

import android.app.Application
import android.util.Log
import android.widget.Toast
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.asCoroutineDispatcher
import okhttp3.OkHttpClient
import okhttp3.Request

@HiltAndroidApp
class MuzicApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
    }

    override fun newImageLoader(): ImageLoader {
        val decodeDispatcher = kotlinx.coroutines.Dispatchers.IO.limitedParallelism(3)
        return ImageLoader.Builder(this)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.20) // ~20% of available memory for reused bitmaps
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05) // ~5% of free disk space
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .crossfade(300)
            .dispatcher(decodeDispatcher)
            .build()
    }
}
