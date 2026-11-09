package com.riztechacademy.mandibhav.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.riztechacademy.mandibhav.ui.theme.MandiBhavTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Google Play asks for the privacy policy inside the app, not only on the
 * store listing. These check that the Alerts tab shows it and links to the
 * full policy. They run on the JVM and on a device, like PricesScreenTest.
 */
@RunWith(AndroidJUnit4::class)
class PrivacyNoticeTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var policyOpened = 0

    private fun showAlerts() {
        compose.setContent {
            MandiBhavTheme {
                AlertsScreen(
                    uiState = AlertsUiState(isLoading = false),
                    showNotificationsOff = false,
                    onCommodityChosen = {},
                    onMarketChosen = {},
                    onDirectionChosen = {},
                    onLevelChange = {},
                    onSave = {},
                    onDelete = {},
                    onOpenSettings = {},
                    onOpenPrivacyPolicy = { policyOpened++ },
                )
            }
        }
    }

    @Test
    fun privacyOpensTheNoticeAndLinksToTheFullPolicy() {
        showAlerts()

        compose.onNodeWithText("Privacy").performClick()
        compose.onNodeWithText(privacySummary.first()).assertIsDisplayed()
        compose.onNodeWithText("Full policy").performClick()

        assertEquals(1, policyOpened)
    }

    @Test
    fun closeHidesTheNotice() {
        showAlerts()

        compose.onNodeWithText("Privacy").performClick()
        compose.onNodeWithText("Close").performClick()

        compose.onNodeWithText(privacySummary.first()).assertDoesNotExist()
    }
}
