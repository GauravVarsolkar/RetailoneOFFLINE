package com.retailone.pos.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.retailone.pos.models.LoginModels.LoginRequest
import com.retailone.pos.models.LoginModels.LoginResponse
import com.retailone.pos.models.LogoutModel.LogoutReq
import com.retailone.pos.models.LogoutModel.LogoutRes
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MPOSLoginViewmodel : ViewModel() {

    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData: LiveData<ProgressData>
        get() = loading


    val logindata = MutableLiveData<LoginResponse>()
    val loginLiveData: LiveData<LoginResponse>
        get() = logindata


    val logoutdata = MutableLiveData<LogoutRes>()
    val logoutLiveData: LiveData<LogoutRes>
        get() = logoutdata


    fun callLoginApi(context: Context, email: String, password: String, device_id: String) {
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiServiceNoToken()
            .mposLogin(LoginRequest(email = email, password = password, device_id = device_id))
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    Log.d("loginResponse", response.body().toString())

                    if (response.isSuccessful && response.body() != null) {
                        logindata.postValue(response.body())

                        loading.postValue(ProgressData(isProgress = false))
                    } else {
                        loading.postValue(
                            ProgressData(
                                isProgress = false,
                                isMessage = true,
                                message = "Something Went Wrong"
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    loading.postValue(
                        ProgressData(
                            isProgress = false,
                            isMessage = true,
                            message = "Something Went Wrong"
                        )
                    )
                }
            })
    }

    fun callLogoutApi(context: Context, storemanager_id: String, device_id: String) {
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context)
            .getLogoutAPI(LogoutReq(user_id = storemanager_id, device_id = device_id))
            .enqueue(object : Callback<LogoutRes> {
                override fun onResponse(call: Call<LogoutRes>, response: Response<LogoutRes>) {

                    if (response.isSuccessful && response.body() != null) {
                        logoutdata.postValue(response.body())
                        loading.postValue(ProgressData(isProgress = false))
                    } else {
                        // force logout
                        logoutdata.postValue(LogoutRes(emptyList(),"",0))

                        loading.postValue(
                            ProgressData(
                                isProgress = false,
                                isMessage = true,
                                message = "You've been logged out"
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<LogoutRes>, t: Throwable) {

                    // force logout
                    logoutdata.postValue(LogoutRes(emptyList(),"",0))

                    loading.postValue(
                        ProgressData(
                            isProgress = false,
                            isMessage = true,
                            message = "You've been logged out"
                        )
                    )
                }
            })
    }
}


/*
when (response.code()) {
    401 -> {
        // Handle unauthorized access
    }
    // Add more cases as needed
    else -> {
        loading.postValue(ProgressData(isProgress = false, isMessage = true, message = "Something Went Wrong"))
    }
}*/
