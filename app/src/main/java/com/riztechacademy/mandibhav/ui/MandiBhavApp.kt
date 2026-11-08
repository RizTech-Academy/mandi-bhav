package com.riztechacademy.mandibhav.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.riztechacademy.mandibhav.R
import com.riztechacademy.mandibhav.navigation.Alerts
import com.riztechacademy.mandibhav.navigation.Market
import com.riztechacademy.mandibhav.navigation.Markets
import com.riztechacademy.mandibhav.navigation.PriceDetail
import com.riztechacademy.mandibhav.navigation.Prices
import com.riztechacademy.mandibhav.navigation.entries
import com.riztechacademy.mandibhav.navigation.rememberTopLevelNavigation

/** A destination in the navigation bar or rail. */
private data class TopLevelDestination(
    val key: NavKey,
    val label: String,
    @param:DrawableRes val icon: Int,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Prices, "Prices", R.drawable.ic_prices),
    TopLevelDestination(Markets, "Markets", R.drawable.ic_markets),
    TopLevelDestination(Alerts, "Alerts", R.drawable.ic_alerts),
)

/**
 * The whole app: a navigation bar on a phone, a navigation rail from 600 dp,
 * and one back stack per destination, shown by NavDisplay.
 */
@Composable
fun MandiBhavApp(
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass,
) {
    val navigation = rememberTopLevelNavigation(
        startDestination = Prices,
        topLevelDestinations = topLevelDestinations.map { it.key },
    )

    val entryProvider = entryProvider<NavKey> {
        entry<Prices> {
            PricesRoute(onPriceClick = { id -> navigation.navigate(PriceDetail(id)) })
        }
        entry<PriceDetail> { key ->
            PriceDetailRoute(priceId = key.priceId, onBack = { navigation.goBack() })
        }
        entry<Markets> {
            MarketsRoute(onMarketClick = { name -> navigation.navigate(Market(name)) })
        }
        entry<Market> { key ->
            MarketRoute(
                market = key.name,
                onPriceClick = { id -> navigation.navigate(PriceDetail(id)) },
                onBack = { navigation.goBack() },
            )
        }
        entry<Alerts> {
            AlertsScreen()
        }
    }

    // One tree for both layouts: the rail is added or left out, and the bar
    // is added or left out, but NavDisplay stays in the same place. If the
    // two layouts were separate branches, NavDisplay would move when the
    // window crossed 600 dp, and every screen's saved state and ViewModel
    // would be looked up under a different position and not found.
    val useRail = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    Row(modifier = modifier.fillMaxSize()) {
        if (useRail) {
            NavigationRail {
                for (destination in topLevelDestinations) {
                    NavigationRailItem(
                        selected = destination.key == navigation.currentTopLevel,
                        onClick = { navigation.navigate(destination.key) },
                        icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        }
        Scaffold(
            modifier = Modifier
                .weight(1f)
                // The rail already keeps clear of the system bars and the
                // camera cutout on its side; without this, each screen
                // would leave the same gap again.
                .then(
                    if (useRail) {
                        Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                    } else {
                        Modifier
                    },
                ),
            // No system insets here: with the bar, the bar handles the bottom;
            // with the rail, each screen handles its own, as it did before.
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        for (destination in topLevelDestinations) {
                            NavigationBarItem(
                                selected = destination.key == navigation.currentTopLevel,
                                onClick = { navigation.navigate(destination.key) },
                                icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavDisplay(
                entries = navigation.entries(entryProvider),
                onBack = { navigation.goBack() },
                // innerPadding is the bar's height, which includes the system
                // navigation bar under it; consuming it stops each screen's
                // own Scaffold adding that inset a second time.
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            )
        }
    }
}
