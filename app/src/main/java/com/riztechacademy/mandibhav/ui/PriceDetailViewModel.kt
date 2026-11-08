package com.riztechacademy.mandibhav.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riztechacademy.mandibhav.data.PriceRepository
import com.riztechacademy.mandibhav.domain.FindBestMarketUseCase
import com.riztechacademy.mandibhav.model.CommodityPrice
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** What the detail screen can show: exactly one of these at a time. */
sealed interface PriceDetailUiState {
    /** The repository has not answered yet. */
    data object Loading : PriceDetailUiState

    /** The id names no price (any more). */
    data object NotFound : PriceDetailUiState

    /** The price, its week, and the best market today for its commodity. */
    data class Found(val detail: PriceWeek, val bestMarket: CommodityPrice?) : PriceDetailUiState
}

/**
 * Holds what the detail screen shows for one price, and keeps it current.
 *
 * It receives only the id, from the navigation key, and asks [repository]
 * for the rest. The key is saved with the back stack, so after process death
 * a new ViewModel gets the same id and loads the price again: there is
 * nothing for this class to keep in a SavedStateHandle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = PriceDetailViewModel.Factory::class)
class PriceDetailViewModel @AssistedInject constructor(
    @Assisted priceId: String,
    repository: PriceRepository,
    findBestMarket: FindBestMarketUseCase,
) : ViewModel() {

    val uiState: StateFlow<PriceDetailUiState> =
        repository.price(priceId).flatMapLatest { price ->
            if (price == null) {
                flowOf(PriceDetailUiState.NotFound)
            } else {
                // The commodity is only known once the price has arrived, so
                // the best-market question is asked inside, and asked again
                // whenever the price changes.
                combine(repository.week(priceId), findBestMarket(price.commodity)) { week, best ->
                    PriceDetailUiState.Found(detail = priceWeek(price, week), bestMarket = best)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PriceDetailUiState.Loading,
        )

    /** How Hilt builds this ViewModel for one id: the id from the key, the rest injected. */
    @AssistedFactory
    interface Factory {
        fun create(priceId: String): PriceDetailViewModel
    }
}
