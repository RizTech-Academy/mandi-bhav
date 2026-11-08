package com.riztechacademy.mandibhav.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The real DataStore, writing a real file in a temporary folder. DataStore
 * needs no Android to run, so this is a plain JVM test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreUserPreferencesRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val file get() = File(folder.root, "user_preferences.preferences_pb")

    /** A repository on [file] whose DataStore lives in [scope]. */
    private fun repositoryIn(scope: CoroutineScope) = DataStoreUserPreferencesRepository(
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
    )

    @Test
    fun noHomeMarketUntilOneIsChosen() = runTest {
        assertNull(repositoryIn(backgroundScope).homeMarket.first())
    }

    @Test
    fun aChosenMarketIsReadBackAndCanBeCleared() = runTest {
        val repository = repositoryIn(backgroundScope)

        repository.setHomeMarket("Lasalgaon APMC")
        assertEquals("Lasalgaon APMC", repository.homeMarket.first())

        repository.setHomeMarket(null)
        assertNull(repository.homeMarket.first())
    }

    @Test
    fun theChoiceIsInTheFileNotInMemory() = runTest {
        // The first DataStore is written to and then shut down, as the app
        // is when its process dies. A new one on the same file is what the
        // app sees on its next cold start.
        val firstLife = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        repositoryIn(firstLife).setHomeMarket("Nashik APMC")
        firstLife.cancel()

        assertEquals("Nashik APMC", repositoryIn(backgroundScope).homeMarket.first())
    }
}
