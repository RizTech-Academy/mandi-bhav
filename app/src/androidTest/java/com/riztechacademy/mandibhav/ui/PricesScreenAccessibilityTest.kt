package com.riztechacademy.mandibhav.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowSizeClass
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.ui.theme.MandiBhavTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Google's Accessibility Test Framework over the prices screen: missing
 * labels, touch targets under 48 dp, low contrast and traversal order.
 *
 * On a device only. Under Robolectric the same checks pass whatever is on
 * the screen: Google's own example of an unlabelled button, which fails
 * here, passed there.
 */
@RunWith(AndroidJUnit4::class)
class PricesScreenAccessibilityTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun thePricesScreenPassesTheAccessibilityChecks() {
        compose.setContent {
            MandiBhavTheme {
                PricesScreen(
                    uiState = PricesUiState(commodities = commoditiesIn(SamplePrices.all), prices = SamplePrices.all),
                    onQueryChange = {},
                    onCommodityClick = {},
                    onClearFilters = {},
                    onRefresh = {},
                    onUserMessageShown = {},
                    onPriceClick = {},
                    windowSizeClass = WindowSizeClass(minWidthDp = 400, minHeightDp = 800),
                )
            }
        }
        compose.enableAccessibilityChecks()

        compose.onRoot().tryPerformAccessibilityChecks()
    }
}
