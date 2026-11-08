package com.riztechacademy.mandibhav.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riztechacademy.mandibhav.data.PriceRepository
import com.riztechacademy.mandibhav.domain.FindBestMarketUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the prices screen and exposes it as one [PricesUiState].
 *
 * The search query and the chosen commodity are kept in [savedStateHandle],
 * so they come back after Android kills the process. The prices are data:
 * they come from the repository, and follow it when it changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PricesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: PriceRepository,
    private val findBestMarket: FindBestMarketUseCase,
) : ViewModel() {

    /** What the refresh button has set going: whether it is running, and what to tell the user. */
    private data class RefreshStatus(val isRefreshing: Boolean = false, val userMessage: String? = null)

    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val selectedCommodity = savedStateHandle.getStateFlow<String?>(KEY_COMMODITY, null)
    private val refreshStatus = MutableStateFlow(RefreshStatus())

    /** The best market for the chosen commodity; switches when the chip does. */
    private val bestMarket = selectedCommodity.flatMapLatest { commodity ->
        if (commodity == null) flowOf(null) else findBestMarket(commodity)
    }

    /** The whole screen. Each input above changes it; nothing else does. */
    val uiState: StateFlow<PricesUiState> =
        combine(
            repository.todaysPrices(), query, selectedCommodity, bestMarket, refreshStatus,
        ) { prices, query, commodity, best, status ->
            PricesUiState(
                isLoading = false,
                query = query,
                selectedCommodity = commodity,
                commodities = commoditiesIn(prices),
                prices = filterPrices(prices, query, commodity),
                bestMarket = best,
                isRefreshing = status.isRefreshing,
                userMessage = status.userMessage,
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
                refreshStatus.update { it.copy(userMessage = message) }
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
