package com.riztechacademy.mandibhav.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.riztechacademy.mandibhav.data.RefreshFailure
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.riztechacademy.mandibhav.R
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.ui.theme.MandiBhavTheme
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch

/**
 * The prices screen as the app uses it: reads the state from [PricesViewModel]
 * and passes plain values to [PricesScreen]. This is the only composable that
 * knows the ViewModel exists, and the only one that is not previewed.
 */
@Composable
fun PricesRoute(
    onPriceClick: (priceId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PricesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PricesScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onCommodityClick = viewModel::onCommodityClick,
        onClearFilters = viewModel::clearFilters,
        onRefresh = viewModel::refresh,
        onUserMessageShown = viewModel::userMessageShown,
        onPriceClick = onPriceClick,
        modifier = modifier,
    )
}

/**
 * The prices screen, stateless: it draws [uiState] and reports every change
 * the user asks for through the `on…` parameters.
 * Only state that belongs to the UI itself (scroll position, the snackbar)
 * is remembered here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricesScreen(
    uiState: PricesUiState,
    onQueryChange: (String) -> Unit,
    onCommodityClick: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onUserMessageShown: () -> Unit,
    onPriceClick: (priceId: String) -> Unit,
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass,
) {
    val columns = if (
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    ) 2 else 1

    val gridState = rememberLazyGridState()
    val layoutDirection = LocalLayoutDirection.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // The index of the first visible item changes a few times per screenful,
    // not on every pixel; derivedStateOf still turns it into one yes or no.
    val showBackToTop by remember {
        derivedStateOf { gridState.firstVisibleItemIndex > BACK_TO_TOP_AFTER_ITEMS }
    }

    // While nothing matches, offer to clear the filters. The effect restarts
    // when noResults changes, and leaving it (results again) cancels it,
    // which dismisses the snackbar.
    val noResults = uiState.noResults
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

    // A one-time message from the ViewModel, such as "3 prices updated.". It
    // stays in the state until it has been shown, so a rotation in the
    // middle shows it again rather than losing it.
    // A failed refresh's message offers "Retry" in the snackbar itself.
    uiState.userMessage?.let { message ->
        val currentOnUserMessageShown by rememberUpdatedState(onUserMessageShown)
        val currentOnRefresh by rememberUpdatedState(onRefresh)
        val canRetry = uiState.refreshFailure != null
        LaunchedEffect(message) {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (canRetry) "Retry" else null,
            )
            currentOnUserMessageShown()
            if (result == SnackbarResult.ActionPerformed) currentOnRefresh()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = onRefresh, enabled = !uiState.isRefreshing) {
                        Text("Refresh")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (showBackToTop) {
                ExtendedFloatingActionButton(
                    onClick = { scope.launch { gridState.animateScrollToItem(0) } },
                ) {
                    Text("Back to top")
                }
            }
        },
    ) { innerPadding ->
        // The thin bar under the app bar is for a refresh over prices that are
        // already on screen. A first download says so in the body instead.
        if (uiState.isLoading || (uiState.isRefreshing && !uiState.isFirstDownload)) {
            LinearProgressIndicator(
                modifier = Modifier
                    .padding(top = innerPadding.calculateTopPadding())
                    .fillMaxWidth(),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            // The top and sides go outside the scroll, so the sticky chips stop
            // under the app bar. The bottom goes inside it: cards scroll behind
            // the gesture handle, and the last one still ends clear of it.
            modifier = Modifier
                .padding(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                )
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .testTag(PRICES_GRID_TAG),
            // 88 dp more at the bottom lets the last card scroll clear of the
            // "Back to top" button (56 dp tall, 16 dp from the edge).
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 88.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "search", contentType = "search", span = { GridItemSpan(maxLineSpan) }) {
                PriceSearchField(
                    query = uiState.query,
                    onQueryChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            stickyHeader(key = "chips", contentType = "chips") {
                // A surface behind the chips, so cards do not show through
                // when they scroll underneath.
                Surface(modifier = Modifier.fillMaxWidth()) {
                    CommodityChips(
                        commodities = uiState.commodities,
                        selected = uiState.selectedCommodity,
                        onCommodityClick = { commodity ->
                            onCommodityClick(commodity)
                            // An event, so it is handled here, not in an effect:
                            // a new filter starts from its first result.
                            scope.launch { gridState.scrollToItem(0) }
                        },
                    )
                }
            }
            if (!uiState.isLoading && !uiState.noSavedPrices) {
                item(key = "updated", contentType = "message", span = { GridItemSpan(maxLineSpan) }) {
                    UpdatedText(lastFetched = uiState.lastFetched, isOnline = uiState.isOnline)
                }
            }
            if (uiState.noSavedPrices) {
                item(key = "nothing-saved", contentType = "message", span = { GridItemSpan(maxLineSpan) }) {
                    NothingSaved(
                        isDownloading = uiState.isRefreshing,
                        failure = uiState.refreshFailure,
                        onRetry = onRefresh,
                    )
                }
            }
            uiState.homeMarket?.let { home ->
                item(key = "home-market", contentType = "message", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "$home, your home market, comes first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            uiState.bestMarket?.let { best ->
                item(key = "best-market", contentType = "message", span = { GridItemSpan(maxLineSpan) }) {
                    BestMarketText(best = best)
                }
            }
            if (noResults) {
                item(key = "no-results", contentType = "message", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "No prices match \"${uiState.query.trim()}\".",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(
                items = uiState.prices,
                key = { price -> price.id },
                contentType = { "price" },
            ) { price ->
                PriceCard(
                    price = price,
                    onClick = { onPriceClick(price.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * How old the saved prices are, in the error colour once they are older than
 * [STALE_AFTER]. "Now" is read when this is drawn; it is not a ticking clock.
 */
@Composable
private fun UpdatedText(lastFetched: Instant?, isOnline: Boolean) {
    val label = updatedLabel(
        lastFetched = lastFetched,
        now = Instant.now(),
        zone = ZoneId.systemDefault(),
        locale = LocalConfiguration.current.locales[0],
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (!isOnline) {
            Text(
                text = "Offline. Showing the prices saved on this phone.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = if (label.isStale && lastFetched != null) "${label.text}. May be out of date." else label.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (label.isStale) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/**
 * The body of a phone with no prices saved: downloading, failed, or waiting.
 * Loading on a slow connection is the case module 7 left open. Under a
 * second it needs nothing more than the spinner; after [SLOW_AFTER_MILLIS] the
 * screen says that it is slow, so a farmer on weak data knows the app has
 * not frozen and that waiting is worth it.
 */
@Composable
private fun NothingSaved(isDownloading: Boolean, failure: RefreshFailure?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when {
            isDownloading -> {
                // Saveable: a rotation half-way through a slow download must not
                // take the explanation away for another five seconds.
                var isSlow by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(SLOW_AFTER_MILLIS)
                    isSlow = true
                }
                CircularProgressIndicator()
                Text("Downloading today's prices…", style = MaterialTheme.typography.titleMedium)
                if (isSlow) {
                    Text(
                        text = "This is taking a while; the connection is slow. Once they arrive, " +
                            "the prices are saved and open instantly next time.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            failure != null -> {
                Text(
                    text = refreshFailureMessage(failure, hasSavedPrices = false),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onRetry) { Text("Try again") }
            }
            else -> Text(
                text = "No prices saved on this phone yet. Tap Refresh to download today's.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The grid's test tag. A lazy grid composes only the cards on screen, so a
 * test scrolls the grid to a card before it can find it.
 */
const val PRICES_GRID_TAG = "prices-grid"

/** How long a first download runs before the screen says the connection is slow. */
private const val SLOW_AFTER_MILLIS = 5_000L

/** Show "Back to top" once the search field and the first two cards have scrolled away. */
private const val BACK_TO_TOP_AFTER_ITEMS = 3

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PricesScreenPreview() {
    MandiBhavTheme {
        PricesScreen(
            uiState = PricesUiState(
                commodities = commoditiesIn(SamplePrices.all),
                prices = SamplePrices.all,
            ),
            onQueryChange = {},
            onCommodityClick = {},
            onClearFilters = {},
            onRefresh = {},
            onUserMessageShown = {},
            onPriceClick = {},
        )
    }
}

@Preview(name = "Filtered: tomato in Pune, refreshing", showBackground = true, showSystemUi = true)
@Composable
private fun PricesScreenFilteredPreview() {
    MandiBhavTheme {
        PricesScreen(
            uiState = PricesUiState(
                query = "pune",
                selectedCommodity = "Tomato",
                commodities = commoditiesIn(SamplePrices.all),
                prices = filterPrices(SamplePrices.all, query = "pune", commodity = "Tomato"),
                bestMarket = SamplePrices.puneTomato,
                isRefreshing = true,
            ),
            onQueryChange = {},
            onCommodityClick = {},
            onClearFilters = {},
            onRefresh = {},
            onUserMessageShown = {},
            onPriceClick = {},
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
            uiState = PricesUiState(
                commodities = commoditiesIn(SamplePrices.all),
                prices = SamplePrices.all,
            ),
            onQueryChange = {},
            onCommodityClick = {},
            onClearFilters = {},
            onRefresh = {},
            onUserMessageShown = {},
            onPriceClick = {},
            windowSizeClass = WindowSizeClass(minWidthDp = 700, minHeightDp = 840),
        )
    }
}
