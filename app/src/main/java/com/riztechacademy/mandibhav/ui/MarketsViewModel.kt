package com.riztechacademy.mandibhav.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riztechacademy.mandibhav.data.PriceRepository
import com.riztechacademy.mandibhav.model.CommodityPrice
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Everything the markets list shows. */
data class MarketsUiState(
    val isLoading: Boolean = false,
    val markets: List<MarketSummary> = emptyList(),
)

/** Everything one market's screen shows. */
data class MarketUiState(
    val market: String,
    val isLoading: Boolean = false,
    val prices: List<CommodityPrice> = emptyList(),
)

/** Holds the markets list: every market that reported today. */
@HiltViewModel
class MarketsViewModel @Inject constructor(repository: PriceRepository) : ViewModel() {
    val uiState: StateFlow<MarketsUiState> =
        repository.todaysPrices()
            .map { prices -> MarketsUiState(markets = marketSummaries(prices)) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MarketsUiState(isLoading = true),
            )
}

/** Holds one market's prices today, for the market named in the navigation key. */
@HiltViewModel(assistedFactory = MarketViewModel.Factory::class)
class MarketViewModel @AssistedInject constructor(
    @Assisted market: String,
    repository: PriceRepository,
) : ViewModel() {
    val uiState: StateFlow<MarketUiState> =
        repository.pricesAt(market)
            .map { prices -> MarketUiState(market = market, prices = prices) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MarketUiState(market = market, isLoading = true),
            )

    @AssistedFactory
    interface Factory {
        fun create(market: String): MarketViewModel
    }
}
