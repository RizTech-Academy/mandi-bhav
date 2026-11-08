package com.riztechacademy.mandibhav.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riztechacademy.mandibhav.data.PriceRepository
import com.riztechacademy.mandibhav.data.UserPreferencesRepository
import com.riztechacademy.mandibhav.model.CommodityPrice
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the markets list shows. */
data class MarketsUiState(
    val isLoading: Boolean = false,
    val markets: List<MarketSummary> = emptyList(),
)

/** Everything one market's screen shows. */
data class MarketUiState(
    val market: String,
    val isLoading: Boolean = false,
    /** The district the market is in; empty until known. */
    val district: String = "",
    val prices: List<CommodityPrice> = emptyList(),
    /** This is the market the user chose as theirs. */
    val isHomeMarket: Boolean = false,
)

/** Holds the markets list: every market that reported today. */
@HiltViewModel
class MarketsViewModel @Inject constructor(repository: PriceRepository) : ViewModel() {
    val uiState: StateFlow<MarketsUiState> =
        repository.markets()
            .map { markets -> MarketsUiState(markets = markets.map { it.summary() }) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MarketsUiState(isLoading = true),
            )
}

/** Holds one market's prices today, for the market named in the navigation key. */
@HiltViewModel(assistedFactory = MarketViewModel.Factory::class)
class MarketViewModel @AssistedInject constructor(
    @Assisted private val market: String,
    repository: PriceRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<MarketUiState> =
        combine(repository.market(market), preferences.homeMarket) { found, home ->
            MarketUiState(
                market = market,
                district = found?.district.orEmpty(),
                prices = found?.prices.orEmpty(),
                isHomeMarket = home == market,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MarketUiState(market = market, isLoading = true),
            )

    /** Makes this market the home market, or clears it if it already is. */
    fun toggleHomeMarket() {
        val isHome = uiState.value.isHomeMarket
        viewModelScope.launch {
            preferences.setHomeMarket(if (isHome) null else market)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(market: String): MarketViewModel
    }
}
