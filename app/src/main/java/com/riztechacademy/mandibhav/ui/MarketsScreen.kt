package com.riztechacademy.mandibhav.ui

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.riztechacademy.mandibhav.ui.permissions.PermissionStatus
import com.riztechacademy.mandibhav.ui.permissions.rememberRuntimePermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.model.Market
import com.riztechacademy.mandibhav.ui.theme.MandiBhavTheme

/**
 * One market in the markets list: its name, its district, how many prices it
 * has today and, once the user's location is known, how far away it is.
 */
data class MarketSummary(
    val name: String,
    val district: String,
    val priceCount: Int,
    val distanceKm: Int? = null,
)

/** What the markets list shows for [this] market. */
fun Market.summary() = MarketSummary(name = name, district = district, priceCount = prices.size)

/** "Nashik district, 2 prices today · 14 km", leaving out what is not known. */
fun MarketSummary.description(): String {
    val count = if (priceCount == 1) "1 price today" else "$priceCount prices today"
    val where = if (district.isEmpty()) count else "$district district, $count"
    return distanceKm?.let { "$where · ${describeKm(it)}" } ?: where
}

/** "under 1 km", "1 km", "14 km". */
fun describeKm(km: Int): String = if (km < 1) "under 1 km" else "$km km"

/**
 * The Markets destination: the markets that reported today, and the nearest
 * one when the user asks. The location permission is asked for at that
 * moment, from the button that needs it, and never before.
 */
@Composable
fun MarketsRoute(
    onMarketClick: (market: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MarketsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val refusedBefore by viewModel.locationRefusedBefore.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Approximate location only: "which market is nearest" needs a town,
    // not a street, and a user is right to refuse an app that asks for more.
    val location = rememberRuntimePermission(
        permission = Manifest.permission.ACCESS_COARSE_LOCATION,
        refusedBefore = refusedBefore,
        onRefusedChange = viewModel::setLocationRefused,
        onGranted = viewModel::findNearest,
    )
    // Back from the location settings the card sent the user to: try again,
    // so the card does not go on saying location is off after it was turned on.
    LifecycleResumeEffect(uiState.nearest) {
        if (uiState.nearest == NearestMarket.LocationOff && location.status == PermissionStatus.Granted) {
            viewModel.findNearest()
        }
        onPauseOrDispose { }
    }
    MarketsScreen(
        markets = uiState.markets,
        nearest = uiState.nearest,
        permission = location.status,
        onUseLocation = location.onFeatureTap,
        onOpenLocationSettings = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
        onMarketClick = onMarketClick,
        modifier = modifier,
    )
    if (location.explaining) {
        AlertDialog(
            onDismissRequest = location.dismissExplanation,
            title = { Text("Find the nearest market?") },
            text = {
                Text(
                    "Mandi Bhav uses your approximate location, to within a few kilometres, to work out " +
                        "which market is closest. It is used once, on this phone, and is not saved or sent anywhere.",
                )
            },
            confirmButton = { TextButton(onClick = location.confirmExplanation) { Text("Continue") } },
            dismissButton = { TextButton(onClick = location.dismissExplanation) { Text("Not now") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketsScreen(
    markets: List<MarketSummary>,
    onMarketClick: (market: String) -> Unit,
    modifier: Modifier = Modifier,
    nearest: NearestMarket = NearestMarket.NotAsked,
    permission: PermissionStatus = PermissionStatus.NotAsked,
    onUseLocation: () -> Unit = {},
    onOpenLocationSettings: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Markets") }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            item(key = "nearest") {
                NearestMarketCard(
                    nearest = nearest,
                    permission = permission,
                    onUseLocation = onUseLocation,
                    onOpenLocationSettings = onOpenLocationSettings,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(items = markets, key = { it.name }) { market ->
                ListItem(
                    headlineContent = { Text(market.name) },
                    supportingContent = { Text(market.description()) },
                    modifier = Modifier.clickable { onMarketClick(market.name) },
                )
                HorizontalDivider()
            }
        }
    }
}

/** What the card above the list says, from the lookup and the permission together. */
data class NearestCardText(val title: String, val body: String, val action: String?)

fun nearestCardText(nearest: NearestMarket, permission: PermissionStatus): NearestCardText = when {
    nearest is NearestMarket.Finding -> NearestCardText("Finding where you are…", "This can take up to 30 seconds.", null)
    nearest is NearestMarket.Found -> NearestCardText(
        title = "Nearest: ${nearest.market}",
        body = "About ${describeKm(nearest.km)} away in a straight line, from your approximate location.",
        action = "Update",
    )
    permission == PermissionStatus.Blocked -> NearestCardText(
        title = "Location is not allowed",
        body = "To find the nearest market, allow Mandi Bhav to use your location in Settings, under Permissions. " +
            "You can still choose a market from the list.",
        action = "Open Settings",
    )
    permission == PermissionStatus.Refused -> NearestCardText(
        title = "Nearest market",
        body = "Without your location, Mandi Bhav cannot tell which market is nearest. You can still choose one from the list.",
        action = "Use my location",
    )
    nearest is NearestMarket.LocationOff -> NearestCardText(
        title = "Your phone's location is off",
        body = "Turn on location to find the nearest market, then try again.",
        action = "Turn on location",
    )
    nearest is NearestMarket.NoMarketLocations -> NearestCardText(
        title = "Market locations not downloaded yet",
        body = "Refresh the prices on the Prices tab, then try again.",
        action = "Try again",
    )
    nearest is NearestMarket.NoFix -> NearestCardText(
        title = "Could not find where you are",
        body = "No location came in time. Try again in a minute, or choose a market from the list.",
        action = "Try again",
    )
    else -> NearestCardText(
        title = "Nearest market",
        body = "Sort the markets by how far they are from you. Mandi Bhav uses your approximate location, once, on this phone.",
        action = "Use my location",
    )
}

@Composable
private fun NearestMarketCard(
    nearest: NearestMarket,
    permission: PermissionStatus,
    onUseLocation: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = nearestCardText(nearest, permission)
    // The permission's own tap handler knows a blocked permission means Settings.
    val onAction = if (nearest is NearestMarket.LocationOff && permission == PermissionStatus.Granted) {
        onOpenLocationSettings
    } else {
        onUseLocation
    }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text.title, style = MaterialTheme.typography.titleMedium)
            Text(text.body, style = MaterialTheme.typography.bodyMedium)
            if (nearest is NearestMarket.Finding) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            text.action?.let { label -> OutlinedButton(onClick = onAction) { Text(label) } }
        }
    }
}

/** One market's prices today. A price opens the same detail screen as on the Prices tab. */
@Composable
fun MarketRoute(
    market: String,
    onPriceClick: (priceId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MarketViewModel = hiltViewModel<MarketViewModel, MarketViewModel.Factory>(
        creationCallback = { factory -> factory.create(market) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    MarketScreen(
        market = uiState.market,
        district = uiState.district,
        prices = uiState.prices,
        onPriceClick = onPriceClick,
        onBack = onBack,
        modifier = modifier,
        isLoading = uiState.isLoading,
        isHomeMarket = uiState.isHomeMarket,
        onHomeMarketClick = viewModel::toggleHomeMarket,
        onShowOnMap = uiState.location?.let { at -> { context.openOnMap(uiState.market, at) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    market: String,
    district: String,
    prices: List<CommodityPrice>,
    onPriceClick: (priceId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isHomeMarket: Boolean = false,
    onHomeMarketClick: () -> Unit = {},
    /** Opens the market in a maps app; null when the market's location is not known. */
    onShowOnMap: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(market) }, navigationIcon = { BackButton(onBack) })
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (district.isNotEmpty()) {
                item(key = "district") {
                    Text(
                        text = "$district district",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(key = "home-market") {
                // A chip, not a plain button: TalkBack reads whether it is
                // selected, so the choice is heard as well as seen.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isHomeMarket,
                        onClick = onHomeMarketClick,
                        label = { Text("My home market") },
                    )
                    onShowOnMap?.let { show -> OutlinedButton(onClick = show) { Text("Show on map") } }
                }
            }
            if (!isLoading && prices.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = "No prices from this market today.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            items(items = prices, key = { it.id }) { price ->
                PriceCard(
                    price = price,
                    onClick = { onPriceClick(price.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MarketsScreenPreview() {
    MandiBhavTheme {
        MarketsScreen(
            markets = SamplePrices.districts.map { (name, district) ->
                MarketSummary(name, district, SamplePrices.all.count { it.market == name })
            },
            onMarketClick = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MarketScreenPreview() {
    MandiBhavTheme {
        MarketScreen(
            market = "Pune (Gultekdi) APMC",
            district = "Pune",
            prices = SamplePrices.all.filter { it.market == "Pune (Gultekdi) APMC" },
            onPriceClick = {},
            onBack = {},
        )
    }
}
