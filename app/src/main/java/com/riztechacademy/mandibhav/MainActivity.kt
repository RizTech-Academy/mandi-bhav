package com.riztechacademy.mandibhav

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.riztechacademy.mandibhav.navigation.Alerts
import com.riztechacademy.mandibhav.navigation.LatestPrice
import com.riztechacademy.mandibhav.notifications.EXTRA_OPEN_ALERTS
import com.riztechacademy.mandibhav.notifications.EXTRA_COMMODITY
import com.riztechacademy.mandibhav.notifications.EXTRA_MARKET
import com.riztechacademy.mandibhav.ui.MandiBhavApp
import com.riztechacademy.mandibhav.ui.theme.MandiBhavTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Where a tapped notification asked to go, until the app has gone there. */
    private var notificationTarget by mutableStateOf<NavKey?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a fresh start. After rotation or process death the intent
        // is the same one, and the back stack already has the screen on it.
        if (savedInstanceState == null) notificationTarget = intent.notificationTarget()
        // The app was already open (singleTop): the tap arrives here.
        addOnNewIntentListener { newIntent -> newIntent.notificationTarget()?.let { notificationTarget = it } }
        setContent {
            MandiBhavTheme {
                MandiBhavApp(
                    modifier = Modifier.fillMaxSize(),
                    notificationTarget = notificationTarget,
                    onNotificationTargetOpened = { notificationTarget = null },
                )
            }
        }
    }

    /** Today's price for a market and commodity, the Alerts tab, or null for an ordinary launch. */
    private fun Intent.notificationTarget(): NavKey? {
        val market = getStringExtra(EXTRA_MARKET)
        val commodity = getStringExtra(EXTRA_COMMODITY)
        return when {
            market != null && commodity != null -> LatestPrice(market, commodity)
            getBooleanExtra(EXTRA_OPEN_ALERTS, false) -> Alerts
            else -> null
        }
    }
}
