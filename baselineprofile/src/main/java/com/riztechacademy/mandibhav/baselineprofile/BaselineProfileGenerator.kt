package com.riztechacademy.mandibhav.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records which classes and methods the app uses while it starts and while
 * the prices scroll. The plugin writes them into
 * app/src/release/generated/baselineProfiles/baseline-prof.txt, which ships
 * inside the APK; Android compiles that code ahead of time when the app is
 * installed, instead of interpreting it on the first few launches.
 *
 * Run with ./gradlew :app:generateBaselineProfile, then commit the file.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE_NAME, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        waitForPrices()
        scrollThePrices()
    }
}
