package com.riztechacademy.mandibhav.domain

import com.riztechacademy.mandibhav.data.FakePriceRepository
import com.riztechacademy.mandibhav.data.SamplePrices
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FindBestMarketUseCaseTest {

    @Test
    fun answersFromTheRepositoryAsItIsNow() = runTest {
        val repository = FakePriceRepository()
        val findBestMarket = FindBestMarketUseCase(repository)
        assertEquals("Lasalgaon APMC", findBestMarket("Onion").first()?.market)

        // The 4 pm prices: Nashik closes at 2,560, above Lasalgaon's 2,525.
        repository.prices = SamplePrices.all.map { price ->
            SamplePrices.closingPrices.firstOrNull { it.id == price.id } ?: price
        }

        assertEquals("Nashik APMC", findBestMarket("Onion").first()?.market)
    }
}
