package com.riztechacademy.mandibhav.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** A [UserPreferencesRepository] in memory: what a test sets is what the app reads. */
class FakeUserPreferencesRepository(homeMarket: String? = null) : UserPreferencesRepository {

    private val home = MutableStateFlow(homeMarket)

    override val homeMarket: Flow<String?> = home

    override suspend fun setHomeMarket(market: String?) {
        home.value = market
    }
}
