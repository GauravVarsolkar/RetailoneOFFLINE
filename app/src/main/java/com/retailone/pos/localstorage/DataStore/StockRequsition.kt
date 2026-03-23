package com.retailone.pos.localstorage.DataStore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map


class StockRequsition(private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("pref")

    companion object {
        val USERNAME = stringPreferencesKey("USER_NAME")
        val AGE = intPreferencesKey("AGE")

        //val SEARCH_RESULTS_LIST = preferencesKey<List<SearchResData>>("search_results_list")

    }


    suspend fun storeUser(name: String, age: Int) {
        context.dataStore.edit {
            it[USERNAME] = name
            it[AGE] = age
        }
    }


    fun getUserName() = context.dataStore.data.map {
        it[USERNAME] ?: ""
    }

    fun getUserAge() = context.dataStore.data.map {
        it[AGE] ?: -1
    }

}