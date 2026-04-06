package com.retailone.pos.localstorage.SharedPreference

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.retailone.pos.models.AttendanceModel.MonthlyAttendanceRes
import com.retailone.pos.models.UserProfileModels.UserProfileResponse

class ProfileAttendanceHelper(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("ProfileAttendanceCache", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveUserProfile(userProfile: UserProfileResponse) {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(userProfile)
        editor.putString("UserProfileData", json)
        editor.apply()
    }

    fun getUserProfile(): UserProfileResponse? {
        val json = sharedPreferences.getString("UserProfileData", null)
        return if (json != null) {
            gson.fromJson(json, UserProfileResponse::class.java)
        } else {
            null
        }
    }

    fun saveMonthlyAttendance(attendance: MonthlyAttendanceRes) {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(attendance)
        editor.putString("MonthlyAttendanceData", json)
        editor.apply()
    }

    fun getMonthlyAttendance(): MonthlyAttendanceRes? {
        val json = sharedPreferences.getString("MonthlyAttendanceData", null)
        return if (json != null) {
            gson.fromJson(json, MonthlyAttendanceRes::class.java)
        } else {
            null
        }
    }
}
