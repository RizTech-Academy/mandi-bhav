package com.riztechacademy.mandibhav.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The user's own choices, kept on the phone until they change them. */
interface UserPreferencesRepository {

    /** The market the user sells at, or null until they choose one. */
    val homeMarket: Flow<String?>

    /** Makes [market] the home market; null clears it. */
    suspend fun setHomeMarket(market: String?)
}

/** [UserPreferencesRepository] in a Preferences DataStore file. */
class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val homeMarket: Flow<String?> = dataStore.data
        .catch { exception ->
            // A file that cannot be read is no reason to take the prices
            // screen down: carry on as if nothing was chosen. Anything that
            // is not an IOException is a bug, so let it through.
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[HOME_MARKET] }
        .distinctUntilChanged()

    override suspend fun setHomeMarket(market: String?) {
        dataStore.edit { preferences ->
            if (market == null) preferences.remove(HOME_MARKET) else preferences[HOME_MARKET] = market
        }
    }

    private companion object {
        val HOME_MARKET = stringPreferencesKey("home_market")
    }
}
