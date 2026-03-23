package com.retailone.pos.viewmodels.DashboardViewodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.retailone.pos.models.Dispatch.DispatchRequest
import com.retailone.pos.models.Dispatch.DispatchResponse
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class ConfirmReturnViewModel : ViewModel() {
    val dispatchLiveData = MutableLiveData<DispatchResponse>()
    val loadingLiveData = MutableLiveData<ProgressData>()

    fun dispatchStock(request: DispatchRequest, context: Context) {
        loadingLiveData.postValue(ProgressData(true))
        val gson = Gson()
        Log.d("SubmitRequestJSON", gson.toJson(request))
        ApiClient().getApiService(context).dispatchStock(request).enqueue(object : Callback<DispatchResponse> {
            override fun onResponse(call: Call<DispatchResponse>, response: Response<DispatchResponse>) {
                Log.d("SubmitResponse", response.body().toString())
                loadingLiveData.postValue(ProgressData(false))
                if (response.isSuccessful && response.body() != null) {
                    dispatchLiveData.postValue(response.body())
                } else {
                    dispatchLiveData.postValue(
                        DispatchResponse(false, "Dispatch failed: ${response.message()}", response.code())
                    )
                }
            }

            override fun onFailure(call: Call<DispatchResponse>, t: Throwable) {
                loadingLiveData.postValue(ProgressData(false))
                Log.d("SubmitResponsetest:",t.localizedMessage)
                dispatchLiveData.postValue(
                    DispatchResponse(false, "Error: ${t.localizedMessage}", 500)
                )
            }
        })
    }
}
