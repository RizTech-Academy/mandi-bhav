package com.riztechacademy.mandibhav.domain

import com.riztechacademy.mandibhav.data.PriceRepository
import com.riztechacademy.mandibhav.model.CommodityPrice
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The best market today for a commodity, kept up to date as prices change.
 * Two screens ask this question; the rule lives once, in [bestMarketFor].
 */
class FindBestMarketUseCase @Inject constructor(private val repository: PriceRepository) {
    operator fun invoke(commodity: String): Flow<CommodityPrice?> =
        repository.todaysPrices()
            .map { prices -> bestMarketFor(prices, commodity) }
            .distinctUntilChanged()
}
