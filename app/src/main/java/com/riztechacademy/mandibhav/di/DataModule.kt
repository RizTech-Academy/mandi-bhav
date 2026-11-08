package com.riztechacademy.mandibhav.di

import com.riztechacademy.mandibhav.data.DataStoreUserPreferencesRepository
import com.riztechacademy.mandibhav.data.ConnectivityManagerNetworkMonitor
import com.riztechacademy.mandibhav.data.NetworkMonitor
import com.riztechacademy.mandibhav.data.PriceNetworkDataSource
import com.riztechacademy.mandibhav.data.PriceRepository
import com.riztechacademy.mandibhav.data.RoomPriceRepository
import com.riztechacademy.mandibhav.data.UserPreferencesRepository
import com.riztechacademy.mandibhav.data.network.RetrofitPriceNetworkDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Tells Hilt which class to hand out when something asks for a
 * [PriceRepository], and where its prices come from.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    /** One repository for the whole app, backed by the database. */
    @Binds
    @Singleton
    abstract fun bindPriceRepository(repository: RoomPriceRepository): PriceRepository

    /** The prices server, through Retrofit. */
    @Binds
    abstract fun bindPriceNetworkDataSource(source: RetrofitPriceNetworkDataSource): PriceNetworkDataSource

    /** The phone's connection, from ConnectivityManager. */
    @Binds
    abstract fun bindNetworkMonitor(monitor: ConnectivityManagerNetworkMonitor): NetworkMonitor

    /** The user's preferences, in DataStore. */
    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        repository: DataStoreUserPreferencesRepository,
    ): UserPreferencesRepository
}
