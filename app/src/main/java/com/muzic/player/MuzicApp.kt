package com.muzic.player

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
        val decodeDispatcher = java.util.concurrent.Executors.newFixedThreadPool(3).asCoroutineDispatcher()
        return ImageLoader.Builder(this)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.15) // ~15-20% of available app memory for standard items
                    .strongReferencesEnabled(true)
                    .build()
            }
            .crossfade(true)
            .crossfade(300)
            .dispatcher(decodeDispatcher)
            .build()
    }
}
