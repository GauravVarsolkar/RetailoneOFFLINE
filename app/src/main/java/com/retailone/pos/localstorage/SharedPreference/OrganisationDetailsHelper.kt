package com.retailone.pos.localstorage.SharedPreference

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.retailone.pos.models.OrganisationDetailsModel.OrganisationData

class OrganisationDetailsHelper  (context: Context){
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("Organisation", Context.MODE_PRIVATE)

    fun saveOrganisationData(organisationData : OrganisationData) {
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(organisationData)
        editor.putString("OrganisationData", json)
        editor.apply()
    }

    fun getOrganisationData(): OrganisationData
    {
        val gson = Gson()
        val json = sharedPreferences.getString("OrganisationData", gson.toJson(OrganisationData("","",
            "",0,"","","","","",0,"","" )))
        return gson.fromJson(json, OrganisationData::class.java)
    }
}