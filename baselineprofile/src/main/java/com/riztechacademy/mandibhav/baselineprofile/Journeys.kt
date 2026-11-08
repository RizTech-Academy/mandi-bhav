package com.riztechacademy.mandibhav.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

/** The app under test. */
const val PACKAGE_NAME = "com.riztechacademy.mandibhav"

/**
 * Waits until the Prices screen shows prices: the "Refresh" button in its
 * top bar, and a card. A first install downloads today's prices first, so
 * this allows for a slow connection.
 */
fun MacrobenchmarkScope.waitForPrices() {
    device.wait(Until.hasObject(By.text("Refresh")), 10_000)
    device.wait(Until.hasObject(By.descContains("rupees per quintal")), 30_000)
}

/** What a farmer does first: looks down today's prices, and back up. */
fun MacrobenchmarkScope.scrollThePrices() {
    val grid = device.findObject(By.scrollable(true)) ?: error("No scrolling list on the Prices screen")
    // Keep clear of the gesture area at the edges, or the swipe goes home.
    grid.setGestureMargin(device.displayWidth / 5)
    repeat(2) {
        grid.fling(Direction.DOWN)
        device.waitForIdle()
    }
    grid.fling(Direction.UP)
    device.waitForIdle()
}
