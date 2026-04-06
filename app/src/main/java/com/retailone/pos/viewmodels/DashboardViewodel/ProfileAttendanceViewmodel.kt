package com.retailone.pos.viewmodels.DashboardViewodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.retailone.pos.models.AttendanceModel.MonthlyAttendanceReq
import com.retailone.pos.models.AttendanceModel.MonthlyAttendanceRes
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.models.UserProfileModels.UserProfileResponse
import com.retailone.pos.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import com.retailone.pos.localstorage.SharedPreference.ProfileAttendanceHelper
import com.retailone.pos.utils.NetworkUtils
import retrofit2.Response

class ProfileAttendanceViewmodel:ViewModel() {

    val attendancedata = MutableLiveData<MonthlyAttendanceRes>()

    val attendance_LiveData : LiveData<MonthlyAttendanceRes>
        get() = attendancedata

    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData : LiveData<ProgressData>
        get() = loading

    val userprofileData = MutableLiveData<UserProfileResponse>()
    val userProfileLiveData : LiveData<UserProfileResponse>
        get() = userprofileData


    fun callUserProfileApi(context: Context){
        loading.postValue(ProgressData(isProgress = true))

        val helper = ProfileAttendanceHelper(context)

        if (!NetworkUtils.isInternetAvailable(context)) {
            val cachedProfile = helper.getUserProfile()
            if (cachedProfile != null) {
                userprofileData.postValue(cachedProfile!!)
            } else {
                loading.postValue(ProgressData(isProgress = false, isMessage = true, message = "No offline profile data found"))
            }
            loading.postValue(ProgressData(isProgress = false))
            return
        }

        ApiClient().getApiService(context).userProfileAPI().enqueue(object :
            Callback<UserProfileResponse> {
            override fun onResponse(call: Call<UserProfileResponse>, response: Response<UserProfileResponse>) {

                if(response.isSuccessful && response.body()!=null){
                    userprofileData.postValue(response.body())
                    helper.saveUserProfile(response.body()!!)
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
                }
            }

            override fun onFailure(call: Call<UserProfileResponse>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }

    fun callMonthlyattendanceApi(context: Context,storemanager_id:String){
        loading.postValue(ProgressData(isProgress = true))

        val helper = ProfileAttendanceHelper(context)

        if (!NetworkUtils.isInternetAvailable(context)) {
            val cachedAttendance = helper.getMonthlyAttendance()
            if (cachedAttendance != null) {
                attendancedata.postValue(cachedAttendance!!)
            } else {
                loading.postValue(ProgressData(isProgress = false, isMessage = true, message = "No offline attendance data found"))
            }
            loading.postValue(ProgressData(isProgress = false))
            return
        }

        ApiClient().getApiService(context).getMonthlyAttendanceAPI(MonthlyAttendanceReq(storemanager_id)).enqueue(object :
            Callback<MonthlyAttendanceRes> {
            override fun onResponse(call: Call<MonthlyAttendanceRes>, response: Response<MonthlyAttendanceRes>) {

                if(response.isSuccessful && response.body()!=null){
                    attendancedata.postValue(response.body())
                    helper.saveMonthlyAttendance(response.body()!!)
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
                }
            }

            override fun onFailure(call: Call<MonthlyAttendanceRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }

}