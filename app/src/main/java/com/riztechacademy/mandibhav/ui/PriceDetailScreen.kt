package com.riztechacademy.mandibhav.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riztechacademy.mandibhav.R
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.model.DayPrice
import com.riztechacademy.mandibhav.ui.theme.MandiBhavTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

private val weekDayFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private val spokenDayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)
private val detailDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/**
 * The detail screen as the app uses it. [priceId] comes from the navigation
 * key; the ViewModel is created with it and is scoped to this back stack
 * entry, so it is cleared when the user goes back.
 */
@Composable
fun PriceDetailRoute(
    priceId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PriceDetailViewModel = hiltViewModel<PriceDetailViewModel, PriceDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(priceId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    PriceDetailScreen(
        uiState = uiState,
        onBack = onBack,
        modifier = modifier,
        onShare = { price -> context.shareText(shareText(price), title = "${price.commodity} at ${price.market}") },
        saleSlip = { price -> SaleSlipSection(price) },
    )
}

/** Today's price for [commodity] at [market], whichever day's it is: what a notification opens. */
@Composable
fun LatestPriceRoute(
    market: String,
    commodity: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LatestPriceViewModel = hiltViewModel<LatestPriceViewModel, LatestPriceViewModel.Factory>(
        creationCallback = { factory -> factory.create(market, commodity) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    PriceDetailScreen(
        uiState = uiState,
        onBack = onBack,
        modifier = modifier,
        onShare = { price -> context.shareText(shareText(price), title = "${price.commodity} at ${price.market}") },
        saleSlip = { price -> SaleSlipSection(price) },
    )
}

/** One price at one market: today's numbers and the last seven days. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceDetailScreen(
    uiState: PriceDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onShare: (CommodityPrice) -> Unit = {},
    /** Below the week: the sale slip for this commodity and market. */
    saleSlip: @Composable (CommodityPrice) -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text((uiState as? PriceDetailUiState.Found)?.detail?.price?.commodity ?: "Price")
                },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (uiState is PriceDetailUiState.Found) {
                        TextButton(onClick = { onShare(uiState.detail.price) }) { Text("Share") }
                    }
                },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            // Draw nothing until the repository answers. From memory that
            // is the next frame; a spinner would only flash.
            PriceDetailUiState.Loading -> Unit
            // The id came from a saved back stack, and the price it named is
            // gone. Say so, rather than crash or show an empty screen.
            PriceDetailUiState.NotFound -> Text(
                text = "This price is no longer available.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
            )
            is PriceDetailUiState.Found -> PriceDetailContent(
                detail = uiState.detail,
                bestMarket = uiState.bestMarket,
                innerPadding = innerPadding,
                saleSlip = saleSlip,
            )
        }
    }
}

/** Today's numbers, the best market, and the week, for a price that exists. */
@Composable
private fun PriceDetailContent(
    detail: PriceWeek,
    bestMarket: CommodityPrice?,
    innerPadding: PaddingValues,
    saleSlip: @Composable (CommodityPrice) -> Unit,
) {
    val price = detail.price
    LazyColumn(
        modifier = Modifier
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
            .fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        price.photoUrl?.let { url ->
            item(key = "photo") {
                // 3:2, as the server's photos are; Coil decodes it at the
                // width it is drawn, at most 600 dp.
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                    error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .aspectRatio(3f / 2f)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
        }
        item(key = "today") {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = price.market, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = detailDateFormat.format(price.date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${formatRupees(price.modalPrice)} per quintal",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .semantics {
                            contentDescription = "${spokenRupees(price.modalPrice)} per quintal"
                        },
                )
                Text(
                    text = "Modal (most common) price",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Min ${formatRupees(price.minPrice)} · Max ${formatRupees(price.maxPrice)}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .semantics {
                            contentDescription = "Range ${spokenRupees(price.minPrice)} to ${spokenRupees(price.maxPrice)}"
                        },
                )
                detail.changeSinceYesterday?.let { change ->
                    Text(text = describeChange(change), style = MaterialTheme.typography.bodyLarge)
                }
                bestMarket?.let { best -> BestMarketText(best = best, here = price) }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Last 7 days, modal price",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
        // Newest day first, like the rest of the app.
        items(items = detail.week.asReversed(), key = { it.date.toEpochDay() }) { day ->
            WeekRow(
                day = day,
                weekRange = detail.weekRange,
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            )
        }
        item(key = "sale-slip") {
            Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(top = 16.dp)) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
                saleSlip(price)
            }
        }
    }
}

/** One day: the date, a bar for where the price sits in the week, and the price. */
@Composable
private fun WeekRow(day: DayPrice, weekRange: LongRange?, modifier: Modifier = Modifier) {
    // How far along the week's range this day is, 0 to 1. A flat week
    // (every day the same) draws every bar full.
    val fraction = if (weekRange == null || weekRange.first == weekRange.last) {
        1f
    } else {
        (day.modalPrice - weekRange.first).toFloat() / (weekRange.last - weekRange.first)
    }
    Row(
        modifier = modifier
            .height(40.dp)
            .clearAndSetSemantics {
                contentDescription = "${spokenDayFormat.format(day.date)}, ${spokenRupees(day.modalPrice)}"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = weekDayFormat.format(day.date),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(96.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp),
        ) {
            Box(
                modifier = Modifier
                    // At least a sliver, so the week's lowest day still shows a bar.
                    .fillMaxWidth(0.1f + 0.9f * fraction)
                    .fillMaxHeight()
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(6.dp),
                    ),
            )
        }
        Text(
            text = formatRupees(day.modalPrice),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** The up arrow in an app bar, for screens that are not top-level destinations. */
@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = "Back",
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PriceDetailScreenPreview() {
    MandiBhavTheme {
        PriceDetailScreen(
            uiState = PriceDetailUiState.Found(
                detail = priceWeek(SamplePrices.lasalgaonOnion, SamplePrices.weekOf(SamplePrices.lasalgaonOnion)),
                bestMarket = SamplePrices.lasalgaonOnion,
            ),
            onBack = {},
        )
    }
}

@Preview(name = "Price no longer available", showBackground = true)
@Composable
private fun PriceDetailMissingPreview() {
    MandiBhavTheme {
        PriceDetailScreen(uiState = PriceDetailUiState.NotFound, onBack = {})
    }
}
