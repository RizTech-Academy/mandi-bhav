package com.riztechacademy.mandibhav

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Starts Hilt's test application instead of MandiBhavApplication, so each
 * @HiltAndroidTest gets a fresh container whose bindings it may replace.
 * Named as testInstrumentationRunner in app/build.gradle.kts.
 *
 * What the test application does not do: MandiBhavApplication's start-up
 * work (StrictMode, the notification channel, scheduling the daily refresh).
 * A test that needs one of those does it itself.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(classLoader: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(classLoader, HiltTestApplication::class.java.name, context)
}
