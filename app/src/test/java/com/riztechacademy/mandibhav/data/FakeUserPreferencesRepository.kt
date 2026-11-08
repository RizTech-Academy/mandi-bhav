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

    private val refused = MutableStateFlow<Set<String>>(emptySet())

    override val refusedPermissions: Flow<Set<String>> = refused

    override suspend fun setPermissionRefused(permission: String, refused: Boolean) {
        this.refused.value = if (refused) this.refused.value + permission else this.refused.value - permission
    }
}
