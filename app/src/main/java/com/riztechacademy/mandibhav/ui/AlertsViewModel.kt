package com.riztechacademy.mandibhav.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riztechacademy.mandibhav.data.AlertRepository
import com.riztechacademy.mandibhav.data.PriceRepository
import com.riztechacademy.mandibhav.model.AlertDirection
import com.riztechacademy.mandibhav.model.PriceAlert
import com.riztechacademy.mandibhav.model.isMetBy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One saved alert, with today's modal price for it; null if that market has no price today. */
data class AlertRow(val alert: PriceAlert, val todaysModalPrice: Long?)

/** "Above ₹2,500 a quintal. Today ₹2,580, past your level." */
fun AlertRow.description(): String {
    val side = if (alert.direction == AlertDirection.Above) "Above" else "Below"
    val today = when {
        todaysModalPrice == null -> "No price today."
        alert.isMetBy(todaysModalPrice) -> "Today ${formatRupees(todaysModalPrice)}, past your level."
        else -> "Today ${formatRupees(todaysModalPrice)}."
    }
    return "$side ${formatRupees(alert.level)} a quintal. $today"
}

/** The new-alert form, as typed so far. */
data class NewAlertForm(
    val commodity: String? = null,
    val market: String? = null,
    val direction: AlertDirection = AlertDirection.Above,
    val level: String = "",
    /** What is wrong with the form, after a Save that could not save; null otherwise. */
    val error: String? = null,
)

/** The whole Alerts screen. */
data class AlertsUiState(
    val isLoading: Boolean = true,
    val alerts: List<AlertRow> = emptyList(),
    /** The commodities with a price today, to choose from. */
    val commodities: List<String> = emptyList(),
    /** The markets with a price today for the chosen commodity. */
    val markets: List<String> = emptyList(),
    val form: NewAlertForm = NewAlertForm(),
    /** Today's modal price for the chosen commodity and market, as a guide for the level. */
    val todaysModalPrice: Long? = null,
)

/**
 * Owns the Alerts screen: the saved alerts, and the form that adds one.
 *
 * The form is kept in [savedStateHandle], so a half-typed alert survives
 * rotation and process death. Whether notifications are allowed is not
 * here: that belongs to the system, and the screen asks it each time it
 * comes back to the front.
 */
@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val alerts: AlertRepository,
    prices: PriceRepository,
) : ViewModel() {

    private val commodity = savedStateHandle.getStateFlow<String?>(KEY_COMMODITY, null)
    private val market = savedStateHandle.getStateFlow<String?>(KEY_MARKET, null)
    private val direction = savedStateHandle.getStateFlow(KEY_DIRECTION, AlertDirection.Above.name)
    private val level = savedStateHandle.getStateFlow(KEY_LEVEL, "")
    private val error = MutableStateFlow<String?>(null)

    private val form = combine(commodity, market, direction, level, error) { commodity, market, direction, level, error ->
        NewAlertForm(commodity, market, AlertDirection.valueOf(direction), level, error)
    }

    val uiState: StateFlow<AlertsUiState> =
        combine(alerts.alerts(), prices.todaysPrices(), form) { saved, todays, form ->
            val byMarket = todays.associateBy { it.market to it.commodity }
            AlertsUiState(
                isLoading = false,
                alerts = saved.map { AlertRow(it, byMarket[it.market to it.commodity]?.modalPrice) },
                commodities = commoditiesIn(todays),
                markets = todays.filter { it.commodity == form.commodity }.map { it.market }.distinct().sorted(),
                form = form,
                todaysModalPrice = byMarket[form.market to form.commodity]?.modalPrice,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AlertsUiState(),
        )

    /** A different commodity clears the market: it may have no price for it. */
    fun onCommodityChosen(chosen: String) {
        if (chosen != commodity.value) {
            savedStateHandle[KEY_COMMODITY] = chosen
            savedStateHandle[KEY_MARKET] = null
        }
        error.value = null
    }

    fun onMarketChosen(chosen: String) {
        savedStateHandle[KEY_MARKET] = chosen
        error.value = null
    }

    fun onDirectionChosen(chosen: AlertDirection) {
        savedStateHandle[KEY_DIRECTION] = chosen.name
    }

    /** Digits only, at most seven: ₹99,99,999 is more than any mandi price. */
    fun onLevelChange(text: String) {
        savedStateHandle[KEY_LEVEL] = text.filter { it.isDigit() }.take(7)
        error.value = null
    }

    /**
     * Saves the alert in the form, if it is complete, and clears the level
     * for the next one. Returns whether it was saved; if not, the form's
     * error says why.
     */
    fun save(): Boolean {
        val commodity = commodity.value ?: return rejected("Choose a commodity.")
        val market = market.value ?: return rejected("Choose a market.")
        val level = level.value.toLongOrNull()?.takeIf { it > 0 }
            ?: return rejected("Enter the price in whole rupees a quintal, such as 2500.")
        val direction = AlertDirection.valueOf(direction.value)
        viewModelScope.launch { alerts.add(commodity, market, direction, level) }
        savedStateHandle[KEY_LEVEL] = ""
        return true
    }

    private fun rejected(problem: String): Boolean {
        error.value = problem
        return false
    }

    fun delete(id: Long) {
        viewModelScope.launch { alerts.delete(id) }
    }

    private companion object {
        const val KEY_COMMODITY = "commodity"
        const val KEY_MARKET = "market"
        const val KEY_DIRECTION = "direction"
        const val KEY_LEVEL = "level"
    }
}
