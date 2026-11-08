package com.riztechacademy.mandibhav.di

import com.riztechacademy.mandibhav.data.PriceRepository
import com.riztechacademy.mandibhav.data.SamplePriceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Tells Hilt which class to hand out when something asks for a
 * [PriceRepository]. Module 8 changes the one line in this file.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    /** One repository for the whole app: the single source of truth. */
    @Binds
    @Singleton
    abstract fun bindPriceRepository(repository: SamplePriceRepository): PriceRepository
}
