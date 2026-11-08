package com.riztechacademy.mandibhav.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * A scope that lives as long as the app's process, for work that must
 * finish even if the screen that started it goes away, such as saving a
 * download. Never cancelled; use it sparingly, and never for work a screen
 * should stop when the user leaves.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    /**
     * SupervisorJob: one failed job does not cancel the scope and every other
     * job in it. Dispatchers.Default: work started here that is not already
     * main-safe runs off the main thread.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
