package com.riztechacademy.mandibhav.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riztechacademy.mandibhav.data.NetworkMonitor
import com.riztechacademy.mandibhav.data.PriceRepository
import com.riztechacademy.mandibhav.data.RefreshException
import com.riztechacademy.mandibhav.data.RefreshFailure
import com.riztechacademy.mandibhav.data.UserPreferencesRepository
import com.riztechacademy.mandibhav.domain.FindBestMarketUseCase
import com.riztechacademy.mandibhav.model.CommodityPrice
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the prices screen and exposes it as one [PricesUiState].
 *
 * The search query and the chosen commodity are kept in [savedStateHandle],
 * so they come back after Android kills the process. The prices are data:
 * they come from the repository, and follow it when it changes.
 *
 * A phone with no prices saved downloads them as soon as the screen is
 * created; after a refresh that failed for want of a connection, the
 * screen tries again by itself when the connection comes back, but only
 * while someone is looking at it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PricesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: PriceRepository,
    private val findBestMarket: FindBestMarketUseCase,
    preferences: UserPreferencesRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    /**
     * What the last refresh did: whether one is running, what to tell the
     * user, and why it failed, if it did. The failure stays until a refresh
     * succeeds, because an empty screen shows it for as long as it is true.
     */
    private data class RefreshStatus(
        val isRefreshing: Boolean = false,
        val userMessage: String? = null,
        val failure: RefreshFailure? = null,
    )

    /** The search field and the chip, together: they always change the list together. */
    private data class Filter(val query: String, val commodity: String?)

    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val selectedCommodity = savedStateHandle.getStateFlow<String?>(KEY_COMMODITY, null)
    private val filter = combine(query, selectedCommodity, ::Filter)
    private val refreshStatus = MutableStateFlow(RefreshStatus())

    /**
     * Whether the phone is online. Part of the screen's state, so it is only
     * watched while the screen is (WhileSubscribed, below). When it comes
     * back after a refresh failed for want of a connection, refresh again.
     */
    private val isOnline = networkMonitor.isOnline.onEach { online ->
        if (online && refreshStatus.value.failure == RefreshFailure.NoConnection) refresh()
    }

    /** The best market for the chosen commodity; switches when the chip does. */
    private val bestMarket = selectedCommodity.flatMapLatest { commodity ->
        if (commodity == null) flowOf(null) else findBestMarket(commodity)
    }

    /** What is saved on the phone: the prices, when they came, and the market to put first. */
    private data class SavedData(
        val prices: List<CommodityPrice>,
        val homeMarket: String?,
        val lastUpdated: Instant?,
    )

    private val savedData = combine(
        repository.todaysPrices(), preferences.homeMarket, repository.lastUpdated(),
    ) { prices, home, lastUpdated ->
        SavedData(prices, home, lastUpdated)
    }

    /** The whole screen. Each input above changes it; nothing else does. */
    val uiState: StateFlow<PricesUiState> =
        combine(
            savedData, filter, bestMarket, refreshStatus, isOnline,
        ) { (prices, home, lastUpdated), (query, commodity), best, status, online ->
            PricesUiState(
                isLoading = false,
                query = query,
                selectedCommodity = commodity,
                commodities = commoditiesIn(prices),
                prices = filterPrices(homeMarketFirst(prices, home), query, commodity),
                homeMarket = home,
                lastUpdated = lastUpdated,
                noSavedPrices = prices.isEmpty(),
                bestMarket = best,
                isRefreshing = status.isRefreshing,
                userMessage = status.userMessage,
                refreshFailure = status.failure,
                isOnline = online,
            )
        }.stateIn(
            scope = viewModelScope,
            // Keep building for 5 s after the screen stops collecting, so a
            // rotation (a second or less) does not restart the work.
            started = SharingStarted.WhileSubscribed(5_000),
            // Until the repository answers, the filter is already known: the
            // search field shows it at once, even after process death.
            initialValue = PricesUiState(
                isLoading = true,
                query = query.value,
                selectedCommodity = selectedCommodity.value,
            ),
        )

    init {
        viewModelScope.launch {
            // A new install has nothing to show, so it downloads today's
            // prices without waiting for a tap. Once per ViewModel: a
            // rotation keeps it, so this does not run again.
            if (repository.todaysPrices().first().isEmpty()) refresh()
        }
    }

    fun onQueryChange(newQuery: String) {
        savedStateHandle[KEY_QUERY] = newQuery
    }

    /** A tap on a chip: null is "All"; tapping the selected chip again clears it. */
    fun onCommodityClick(commodity: String?) {
        savedStateHandle[KEY_COMMODITY] =
            if (commodity == selectedCommodity.value) null else commodity
    }

    fun clearFilters() {
        savedStateHandle[KEY_QUERY] = ""
        savedStateHandle[KEY_COMMODITY] = null
    }

    /**
     * Fetches the latest prices and says how many changed. The new prices
     * arrive through the repository's flow, not from here. A tap while a
     * refresh is already running is ignored, so the user gets one answer.
     */
    fun refresh() {
        if (refreshStatus.value.isRefreshing) return
        refreshStatus.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                val changed = repository.refresh()
                val message = when (changed) {
                    0 -> "Prices are up to date."
                    1 -> "1 price updated."
                    else -> "$changed prices updated."
                }
                refreshStatus.update { it.copy(userMessage = message, failure = null) }
            } catch (e: RefreshException) {
                // Only the data layer's own failure is caught. Anything else
                // is a bug and should crash in testing; a cancellation must
                // go on up, or the coroutine would carry on after its screen.
                val hasSavedPrices = repository.todaysPrices().first().isNotEmpty()
                refreshStatus.update {
                    it.copy(
                        failure = e.failure,
                        // An empty screen says it in its body; a snackbar would repeat it.
                        userMessage = if (hasSavedPrices) refreshFailureMessage(e.failure, hasSavedPrices = true) else null,
                    )
                }
            } finally {
                refreshStatus.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /** The screen has shown [PricesUiState.userMessage]; stop asking it to. */
    fun userMessageShown() {
        refreshStatus.update { it.copy(userMessage = null) }
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_COMMODITY = "commodity"
    }
}
