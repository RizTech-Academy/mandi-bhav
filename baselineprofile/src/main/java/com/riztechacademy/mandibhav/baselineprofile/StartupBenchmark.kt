package com.riztechacademy.mandibhav.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold start and scrolling, measured twice: with no ahead-of-time
 * compilation (a fresh install from anywhere but Google Play, before
 * Android has learnt anything about the app) and with the Baseline Profile.
 * Each test runs the app ten times and reports the minimum, median and
 * maximum.
 *
 * Numbers from an emulator are indicative only: its "phone" shares a
 * laptop's processor. Compare the two modes on one device; never compare
 * an emulator's milliseconds with a phone's.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupWithoutCompilation() = startup(CompilationMode.None())

    @Test
    fun startupWithBaselineProfile() = startup(CompilationMode.Partial(BaselineProfileMode.Require))

    @Test
    fun scrollWithoutCompilation() = scroll(CompilationMode.None())

    @Test
    fun scrollWithBaselineProfile() = scroll(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun startup(mode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = mode,
        startupMode = StartupMode.COLD,
        iterations = 10,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
        waitForPrices()
    }

    private fun scroll(mode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = mode,
        startupMode = StartupMode.WARM,
        iterations = 10,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            waitForPrices()
        },
    ) {
        scrollThePrices()
    }
}
