package com.riztechacademy.mandibhav.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.riztechacademy.mandibhav.ui.theme.MandiBhavTheme

/** One market in the markets list: its name and how many prices it has today. */
data class MarketSummary(val name: String, val priceCount: Int)

/** Every market in [prices], each once, in the order it first appears. */
fun marketSummaries(prices: List<CommodityPrice>): List<MarketSummary> =
    prices.groupBy { it.market }.map { (name, atMarket) -> MarketSummary(name, atMarket.size) }

/** The Markets destination: the markets that reported today. */
@Composable
fun MarketsRoute(
    onMarketClick: (market: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MarketsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MarketsScreen(markets = uiState.markets, onMarketClick = onMarketClick, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketsScreen(
    markets: List<MarketSummary>,
    onMarketClick: (market: String) -> Unit,
    modifier: Modifier = Modifier,
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
            items(items = markets, key = { it.name }) { market ->
                ListItem(
                    headlineContent = { Text(market.name) },
                    supportingContent = {
                        Text(if (market.priceCount == 1) "1 price today" else "${market.priceCount} prices today")
                    },
                    modifier = Modifier.clickable { onMarketClick(market.name) },
                )
                HorizontalDivider()
            }
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
    MarketScreen(uiState.market, uiState.prices, onPriceClick, onBack, modifier, isLoading = uiState.isLoading)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    market: String,
    prices: List<CommodityPrice>,
    onPriceClick: (priceId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
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
        MarketsScreen(markets = marketSummaries(SamplePrices.all), onMarketClick = {})
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MarketScreenPreview() {
    MandiBhavTheme {
        MarketScreen(
            market = "Pune (Gultekdi) APMC",
            prices = SamplePrices.all.filter { it.market == "Pune (Gultekdi) APMC" },
            onPriceClick = {},
            onBack = {},
        )
    }
}
