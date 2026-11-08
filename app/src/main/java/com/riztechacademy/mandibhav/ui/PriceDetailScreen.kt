package com.riztechacademy.mandibhav.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riztechacademy.mandibhav.R
import com.riztechacademy.mandibhav.data.PriceSource
import com.riztechacademy.mandibhav.data.SamplePrices
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
    viewModel: PriceDetailViewModel = viewModel { PriceDetailViewModel(priceId) },
) {
    PriceDetailScreen(detail = viewModel.detail, onBack = onBack, modifier = modifier)
}

/** One price at one market: today's numbers and the last seven days. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceDetailScreen(
    detail: PriceWeek?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(detail?.price?.commodity ?: "Price") },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { innerPadding ->
        if (detail == null) {
            // The id came from a saved back stack, and the price it named is
            // gone. Say so, rather than crash or show an empty screen.
            Text(
                text = "This price is no longer available.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
            )
            return@Scaffold
        }
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
    val source = PriceSource()
    val id = SamplePrices.lasalgaonOnion.id
    MandiBhavTheme {
        PriceDetailScreen(
            detail = priceWeek(SamplePrices.lasalgaonOnion, source.week(id)),
            onBack = {},
        )
    }
}

@Preview(name = "Price no longer available", showBackground = true)
@Composable
private fun PriceDetailMissingPreview() {
    MandiBhavTheme {
        PriceDetailScreen(detail = null, onBack = {})
    }
}
