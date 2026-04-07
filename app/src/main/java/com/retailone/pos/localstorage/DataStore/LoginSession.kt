package com.retailone.pos.localstorage.DataStore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

class LoginSession (private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("pref")

    companion object {

        private var instance: LoginSession? = null

        fun getInstance(context: Context): LoginSession {
            return instance ?: synchronized(this) {
                instance ?: LoginSession(context).also { instance = it }
            }
        }
        val TOKEN = stringPreferencesKey("LOGIN_TOKEN")
        val IsLogin = booleanPreferencesKey("LOGIN_STATUS")

        val storeID = stringPreferencesKey("STORE_ID")
        val storeManagerID = stringPreferencesKey("STORE_MANAGER_ID")
        val startTotalizer = stringPreferencesKey("START_TOT")
        val startTOTMode = stringPreferencesKey("START_TOT_MODE")
        val cashupDateTime = stringPreferencesKey("CASHUP_DATE_TIME")
        val modulesKey = stringPreferencesKey("ORG_MODULES")
        val isFreshLogin = booleanPreferencesKey("IS_FRESH_LOGIN")
        val spotDiscountEnabled = booleanPreferencesKey("SPOT_DISCOUNT_ENABLED")
        val spotDiscountLimit = stringPreferencesKey("SPOT_DISCOUNT_LIMIT")
    }


    suspend fun storeLoginSession(token:String,loginStatus:Boolean){
        context.dataStore.edit {
            it[TOKEN] = token
            it[IsLogin] = loginStatus
        }
    }



    suspend fun storeToken(token:String){
        context.dataStore.edit {
            it[TOKEN] = token
        }
    }

    suspend fun storeLoginStatus(loginStatus:Boolean){
        context.dataStore.edit {
            it[IsLogin] = loginStatus
        }
    }

    suspend fun storeCashupDateTime(dateTime: String) {
        context.dataStore.edit {
            it[cashupDateTime] = dateTime
        }
    }

    fun getCashupDateTime() = context.dataStore.data.map {
        it[cashupDateTime] ?: ""
    }
    suspend fun storeSpotDiscount(isEnabled: Boolean, maxLimit: String) {
        context.dataStore.edit {
            it[spotDiscountEnabled] = isEnabled
            it[spotDiscountLimit] = maxLimit
        }
    }
    fun isSpotDiscountEnabled() = context.dataStore.data.map {
        it[spotDiscountEnabled] ?: false
    }

    fun getSpotDiscountLimit() = context.dataStore.data.map {
        it[spotDiscountLimit] ?: "0.00"
    }

    suspend fun setFreshLogin(value: Boolean) {
        context.dataStore.edit {
            it[isFreshLogin] = value
        }
    }


    fun getFreshLogin() = context.dataStore.data.map {
        it[isFreshLogin] ?: false
    }


     fun getToken() = context.dataStore.data.map {
        //it[TOKEN] ?: null
        it[TOKEN] ?: ""
    }

    fun getLoginStatus() = context.dataStore.data.map {
        it[IsLogin] ?: false
    }

    suspend fun clearLoginSession() {
        context.dataStore.edit {
            it.remove(TOKEN)
            it.remove(IsLogin)
            it.remove(modulesKey)
        }
    }

    suspend fun saveStoreID(storeid:String){
        context.dataStore.edit {
            it[storeID] = storeid
        }
    }
    fun getStoreID() = context.dataStore.data.map {
        it[storeID] ?: ""
    }
    suspend fun saveModules(modules: List<String>) {
        context.dataStore.edit {
            it[modulesKey] = modules.joinToString(",")
        }
    }

    fun getModules() = context.dataStore.data.map {
        val raw = it[modulesKey] ?: ""
        if (raw.isEmpty()) emptyList()
        else raw.split(",").map { m -> m.trim() }
    }


    suspend fun saveStoreManagerID(store_manager_id:String){
        context.dataStore.edit {
            it[storeManagerID] = store_manager_id
        }
    }

    suspend fun saveStartTOTData(startTOT:String,startTotalizerMode:String){
        context.dataStore.edit {
            it[startTotalizer] = startTOT
            it[startTOTMode]= startTotalizerMode
        }
    }

    fun getStartTOTValue() = context.dataStore.data.map {
        it[startTotalizer]?:""
        it[startTOTMode]?:""
    }


    fun getStoreManagerID() = context.dataStore.data.map {
        it[storeManagerID] ?: ""
    }




}