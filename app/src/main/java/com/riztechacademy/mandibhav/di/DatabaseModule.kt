package com.riztechacademy.mandibhav.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.riztechacademy.mandibhav.data.local.AlertDao
import com.riztechacademy.mandibhav.data.local.MIGRATION_1_2
import com.riztechacademy.mandibhav.data.local.MandiBhavDatabase
import com.riztechacademy.mandibhav.data.local.PriceDao
import com.riztechacademy.mandibhav.data.local.SaleSlipDao
import dagger.Module
import java.time.Clock
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** How Hilt builds the database: once, for the life of the process. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MandiBhavDatabase =
        Room.databaseBuilder<MandiBhavDatabase>(context, "mandi-bhav.db")
            .setDriver(AndroidSQLiteDriver())
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun providePriceDao(database: MandiBhavDatabase): PriceDao = database.priceDao()

    @Provides
    fun provideAlertDao(database: MandiBhavDatabase): AlertDao = database.alertDao()

    @Provides
    fun provideSaleSlipDao(database: MandiBhavDatabase): SaleSlipDao = database.saleSlipDao()

    /** The time, injected so a test can fix it. */
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
