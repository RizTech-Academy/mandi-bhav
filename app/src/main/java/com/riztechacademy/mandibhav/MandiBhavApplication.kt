package com.riztechacademy.mandibhav

import android.app.Application
import android.os.StrictMode
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.riztechacademy.mandibhav.notifications.createNotificationChannels
import com.riztechacademy.mandibhav.work.DailyRefresh
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import java.time.Clock
import java.time.ZonedDateTime
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
 * It gives WorkManager Hilt's worker factory, so a worker gets its
 * dependencies injected; the manifest turns off WorkManager's own start-up
 * initialiser, which would use the default factory instead. On every start
 * it creates the notification channel and makes sure the daily refresh is
 * scheduled.
 *
 * Debug builds turn on StrictMode for the main thread: any disk or network
 * access, or a call a library marks as slow, is written to logcat under the
 * tag StrictMode, with the stack that made it.
 */
@HiltAndroidApp
class MandiBhavApplication : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    @Inject
    lateinit var okHttpClient: Lazy<OkHttpClient>

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var clock: Clock

    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().penaltyDeathOnNetwork().build())
        }
        super.onCreate()
        createNotificationChannels(this)
        // Cheap on the main thread: enqueueing hands the write to WorkManager's
        // own background thread, and StrictMode reports nothing here.
        DailyRefresh.schedule(WorkManager.getInstance(this), ZonedDateTime.now(clock))
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            // Debug builds log what WorkManager decides, under tags starting WM-.
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .build()

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient.get() }))
            }
            .build()
}
