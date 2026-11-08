package com.riztechacademy.mandibhav.ui

import android.Manifest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riztechacademy.mandibhav.data.LocationResult
import com.riztechacademy.mandibhav.data.LocationSource
import com.riztechacademy.mandibhav.data.PriceRepository
import com.riztechacademy.mandibhav.data.UserPreferencesRepository
import com.riztechacademy.mandibhav.domain.nearestMarkets
import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.model.GeoPoint
import com.riztechacademy.mandibhav.model.Market
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the markets list shows. */
data class MarketsUiState(
    val isLoading: Boolean = false,
    /** By name; nearest first once the user's location is known. */
    val markets: List<MarketSummary> = emptyList(),
    val nearest: NearestMarket = NearestMarket.NotAsked,
)

/** Where the "which market is nearest?" question stands. */
sealed interface NearestMarket {
    /** Not asked on this screen yet. Nothing about location has happened. */
    data object NotAsked : NearestMarket

    /** Waiting for a fix. */
    data object Finding : NearestMarket

    /** The nearest market with a known location, [km] away in a straight line. */
    data class Found(val market: String, val km: Int) : NearestMarket

    /** The permission went away between the tap and the fix. */
    data object NoPermission : NearestMarket

    /** Location is off for the whole phone. */
    data object LocationOff : NearestMarket

    /** No fix in time. */
    data object NoFix : NearestMarket

    /**
     * A fix, but no market has a location to measure to: the saved markets
     * came from a download made before the server sent locations.
     */
    data object NoMarketLocations : NearestMarket
}

/** Everything one market's screen shows. */
data class MarketUiState(
    val market: String,
    val isLoading: Boolean = false,
    /** The district the market is in; empty until known. */
    val district: String = "",
    val prices: List<CommodityPrice> = emptyList(),
    /** This is the market the user chose as theirs. */
    val isHomeMarket: Boolean = false,
    /** Roughly where the market is, for directions; null if the server has not said. */
    val location: GeoPoint? = null,
)

/**
 * Holds the markets list: every market that reported today, and, once the
 * user asks, how far each one is.
 *
 * The location is kept here, in memory, for as long as the screen needs it,
 * and nowhere else: it is not saved, not logged and not sent anywhere. After
 * process death the list is back in name order until the user asks again.
 */
@HiltViewModel
class MarketsViewModel @Inject constructor(
    repository: PriceRepository,
    private val locationSource: LocationSource,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    /** Whether the user has ever refused location: Android does not say, so the app remembers. */
    val locationRefusedBefore: StateFlow<Boolean> = preferences.refusedPermissions
        .map { Manifest.permission.ACCESS_COARSE_LOCATION in it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setLocationRefused(refused: Boolean) {
        viewModelScope.launch { preferences.setPermissionRefused(Manifest.permission.ACCESS_COARSE_LOCATION, refused) }
    }

    /** null until asked; the answer of the last lookup otherwise. */
    private val lookup = MutableStateFlow<LocationResult?>(null)
    private val finding = MutableStateFlow(false)

    val uiState: StateFlow<MarketsUiState> =
        combine(repository.markets(), lookup, finding) { markets, result, isFinding ->
            marketsUiState(markets, result, isFinding)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MarketsUiState(isLoading = true),
            )

    /** Finds where the phone is and sorts the markets by distance. Call it only with the permission. */
    fun findNearest() {
        if (finding.value) return
        finding.value = true
        viewModelScope.launch {
            try {
                lookup.value = locationSource.currentLocation()
            } finally {
                finding.value = false
            }
        }
    }
}

/** The list, nearest first if [result] found the phone, with the card's state. */
fun marketsUiState(markets: List<Market>, result: LocationResult?, isFinding: Boolean): MarketsUiState {
    val here = (result as? LocationResult.Found)?.point
    val distances = here?.let { point -> nearestMarkets(point, markets.associate { it.name to it.location }) }
        .orEmpty()
        .associate { it.market to it.km }
    val summaries = markets.map { market ->
        market.summary().copy(distanceKm = distances[market.name]?.roundToInt())
    }
    val nearest = when {
        isFinding -> NearestMarket.Finding
        result == null -> NearestMarket.NotAsked
        result is LocationResult.Found -> distances.minByOrNull { it.value }
            ?.let { (name, km) -> NearestMarket.Found(name, km.roundToInt()) }
            ?: NearestMarket.NoMarketLocations
        result == LocationResult.NoPermission -> NearestMarket.NoPermission
        result == LocationResult.LocationOff -> NearestMarket.LocationOff
        else -> NearestMarket.NoFix
    }
    return MarketsUiState(
        markets = if (distances.isEmpty()) summaries else summaries.sortedBy { it.distanceKm ?: Int.MAX_VALUE },
        nearest = nearest,
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
                location = found?.location,
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
