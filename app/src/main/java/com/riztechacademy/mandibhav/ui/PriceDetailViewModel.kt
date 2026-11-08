package com.riztechacademy.mandibhav.ui

import androidx.lifecycle.ViewModel
import com.riztechacademy.mandibhav.data.PriceSource

/**
 * Holds what the detail screen shows for one price.
 *
 * It receives only the id, from the navigation key, and asks [source] for
 * the rest. The key is saved with the back stack, so after process death a
 * new ViewModel gets the same id and loads the price again: there is
 * nothing for this class to keep in a SavedStateHandle.
 */
class PriceDetailViewModel(
    priceId: String,
    source: PriceSource = PriceSource(),
) : ViewModel() {

    /** The price and its week, or null if [priceId] is not a price (any more). */
    val detail: PriceWeek? = source.price(priceId)?.let { price ->
        priceWeek(price, source.week(priceId))
    }
}
