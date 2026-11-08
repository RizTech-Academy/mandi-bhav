package com.riztechacademy.mandibhav.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import com.riztechacademy.mandibhav.R
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.ui.theme.MandiBhavTheme
import kotlinx.coroutines.launch

/**
 * The prices screen as the app uses it: reads the state from [PricesViewModel]
 * and passes plain values to [PricesScreen]. This is the only composable that
 * knows the ViewModel exists, and the only one that is not previewed.
 */
@Composable
fun PricesRoute(
    modifier: Modifier = Modifier,
    viewModel: PricesViewModel = viewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selectedCommodity by viewModel.selectedCommodity.collectAsStateWithLifecycle()
    val visiblePrices by viewModel.visiblePrices.collectAsStateWithLifecycle()

    PricesScreen(
        prices = visiblePrices,
        commodities = viewModel.commodities,
        query = query,
        selectedCommodity = selectedCommodity,
        onQueryChange = viewModel::onQueryChange,
        onCommodityClick = viewModel::onCommodityClick,
        onClearFilters = viewModel::clearFilters,
        modifier = modifier,
    )
}

/**
 * The prices screen, stateless: it shows [prices] (already filtered) and
 * reports every change the user asks for through the `on…` parameters.
 * Only state that belongs to the UI itself (scroll position, the snackbar)
 * is remembered here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricesScreen(
    prices: List<CommodityPrice>,
    commodities: List<String>,
    query: String,
    selectedCommodity: String?,
    onQueryChange: (String) -> Unit,
    onCommodityClick: (String?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass,
) {
    val columns = if (
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    ) 2 else 1

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Recomposes this screen only when the answer flips, not on every
    // pixel of scrolling.
    val backToTopAfterPx = with(LocalDensity.current) { 400.dp.toPx() }
    val showBackToTop by remember(backToTopAfterPx) {
        derivedStateOf { scrollState.value > backToTopAfterPx }
    }

    // While nothing matches, offer to clear the filters. The effect restarts
    // when noResults changes, and leaving it (results again) cancels it,
    // which dismisses the snackbar.
    val noResults = prices.isEmpty()
    val currentOnClearFilters by rememberUpdatedState(onClearFilters)
    LaunchedEffect(noResults) {
        if (noResults) {
            val result = snackbarHostState.showSnackbar(
                message = "No prices match these filters.",
                actionLabel = "Clear filters",
                duration = SnackbarDuration.Indefinite,
            )
            if (result == SnackbarResult.ActionPerformed) {
                currentOnClearFilters()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.app_name)) })
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (showBackToTop) {
                ExtendedFloatingActionButton(
                    onClick = { scope.launch { scrollState.animateScrollTo(0) } },
                ) {
                    Text("Back to top")
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(scrollState)
                // 88 dp at the bottom lets the last card scroll clear of the
                // "Back to top" button (56 dp tall, 16 dp from the edge).
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PriceSearchField(
                query = query,
                onQueryChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
            )
            CommodityChips(
                commodities = commodities,
                selected = selectedCommodity,
                onCommodityClick = onCommodityClick,
            )
            if (noResults) {
                Text(
                    text = "No prices match \"${query.trim()}\".",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            for (rowOfPrices in prices.chunked(columns)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    for (price in rowOfPrices) {
                        PriceCard(
                            price = price,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                    // An odd last row keeps its card at column width.
                    repeat(columns - rowOfPrices.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PricesScreenPreview() {
    MandiBhavTheme {
        PricesScreen(
            prices = SamplePrices.all,
            commodities = commoditiesIn(SamplePrices.all),
            query = "",
            selectedCommodity = null,
            onQueryChange = {},
            onCommodityClick = {},
            onClearFilters = {},
        )
    }
}

@Preview(name = "Filtered: tomato in Pune", showBackground = true, showSystemUi = true)
@Composable
private fun PricesScreenFilteredPreview() {
    MandiBhavTheme {
        PricesScreen(
            prices = filterPrices(SamplePrices.all, query = "pune", commodity = "Tomato"),
            commodities = commoditiesIn(SamplePrices.all),
            query = "pune",
            selectedCommodity = "Tomato",
            onQueryChange = {},
            onCommodityClick = {},
            onClearFilters = {},
        )
    }
}

@Preview(name = "Unfolded, 700 dp", showBackground = true, widthDp = 700, heightDp = 840)
@Composable
private fun PricesScreenWidePreview() {
    MandiBhavTheme {
        // Passed explicitly, so the preview does not depend on how the
        // preview tool reports its window size.
        PricesScreen(
            prices = SamplePrices.all,
            commodities = commoditiesIn(SamplePrices.all),
            query = "",
            selectedCommodity = null,
            onQueryChange = {},
            onCommodityClick = {},
            onClearFilters = {},
            windowSizeClass = WindowSizeClass(minWidthDp = 700, minHeightDp = 840),
        )
    }
}
