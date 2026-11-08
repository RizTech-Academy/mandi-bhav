package com.riztechacademy.mandibhav

import com.riztechacademy.mandibhav.data.PriceRepository
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.di.PriceRepositoryModule
import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.model.DayPrice
import com.riztechacademy.mandibhav.model.Market
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.time.Instant
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * In every instrumented test, the app gets [StaticPriceRepository] instead
 * of the Room one: known prices, no network, nothing written to the phone.
 * Every other binding stays real.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [PriceRepositoryModule::class])
object TestPriceRepositoryModule {
    @Provides
    @Singleton
    fun providePriceRepository(): PriceRepository = StaticPriceRepository()
}

/** The sample prices, for ever. A refresh changes nothing. */
class StaticPriceRepository(prices: List<CommodityPrice> = SamplePrices.all) : PriceRepository {
    private val current = MutableStateFlow(prices)

    override fun todaysPrices(): Flow<List<CommodityPrice>> = current

    override fun price(id: String): Flow<CommodityPrice?> = current.map { all -> all.firstOrNull { it.id == id } }

    override fun markets(): Flow<List<Market>> = current.map { all ->
        all.groupBy { it.market }
            .map { (name, prices) -> Market(name, SamplePrices.districts[name].orEmpty(), prices, SamplePrices.locations[name]) }
            .sortedBy { it.name }
    }

    override fun market(name: String): Flow<Market?> = markets().map { markets -> markets.firstOrNull { it.name == name } }

    override fun week(id: String): Flow<List<DayPrice>> = flowOf(emptyList())

    override fun lastUpdated(): Flow<Instant?> = flowOf(Instant.parse("2026-11-07T10:35:00Z"))

    override suspend fun refresh(): Int = 0
}
