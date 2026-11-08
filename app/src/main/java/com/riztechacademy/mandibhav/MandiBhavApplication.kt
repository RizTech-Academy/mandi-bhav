package com.riztechacademy.mandibhav

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * The app's Application class. @HiltAndroidApp makes Hilt generate the
 * app-wide container here, so it lives exactly as long as the process.
 */
@HiltAndroidApp
class MandiBhavApplication : Application()
