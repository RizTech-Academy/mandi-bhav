package com.riztechacademy.mandibhav

import android.app.Application
import android.os.StrictMode
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import okhttp3.OkHttpClient

/**
 * The app's Application class. @HiltAndroidApp makes Hilt generate the
 * app-wide container here, so it lives exactly as long as the process.
 *
 * It also builds Coil's one image loader, the first time a picture is
 * asked for, over the same OkHttp client as Retrofit: one connection pool,
 * one set of timeouts, and the debug log shows pictures and prices alike.
 *
 * Debug builds turn on StrictMode for the main thread: any disk or network
 * access, or a call a library marks as slow, is written to logcat under the
 * tag StrictMode, with the stack that made it.
 */
@HiltAndroidApp
class MandiBhavApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().penaltyDeathOnNetwork().build())
        }
        super.onCreate()
    }

    @Inject
    lateinit var okHttpClient: Lazy<OkHttpClient>

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient.get() }))
            }
            .build()
}
