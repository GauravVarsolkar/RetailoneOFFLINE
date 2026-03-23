package com.retailone.pos.localstorage.SharedPreference

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.retailone.pos.models.LocalizationModel.LocalizationData

class LocalizationHelper (context: Context){

        private val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("Localization", Context.MODE_PRIVATE)

        fun saveLocalizationData(localizationData: LocalizationData) {
            val editor = sharedPreferences.edit()
            val gson = Gson()
            val json = gson.toJson(localizationData)
            editor.putString("LocalizationData", json)
            editor.apply()
        }

    fun getLocalizationData(): LocalizationData
    {
        val gson = Gson()
        val json = sharedPreferences.getString("LocalizationData", gson.toJson(LocalizationData("$","d-m-Y","0","CAT")))
        return gson.fromJson(json, LocalizationData::class.java)
    }

       /* fun getLocalizationData(): LocalizationData? {
            val gson = Gson()
            val json = sharedPreferences.getString("LocalizationData", null)
            return gson.fromJson(json, LocalizationData::class.java)
        }*/

        // You can add additional methods to save and retrieve individual fields if needed.

}