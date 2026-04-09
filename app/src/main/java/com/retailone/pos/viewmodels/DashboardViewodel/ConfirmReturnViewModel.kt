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
import kotlinx.coroutines.*
import com.retailone.pos.utils.NetworkUtils

class ConfirmReturnViewModel : ViewModel() {
    val dispatchLiveData = MutableLiveData<DispatchResponse>()
    val loadingLiveData = MutableLiveData<ProgressData>()
    private var repository: com.retailone.pos.localstorage.RoomDB.PendingDispatchRepository? = null

    fun initRepository(context: Context) {
        val database = com.retailone.pos.localstorage.RoomDB.PosDatabase.getDatabase(context)
        repository = com.retailone.pos.localstorage.RoomDB.PendingDispatchRepository(database.pendingDispatchDao())
    }

    fun dispatchStock(request: DispatchRequest, context: Context) {
        loadingLiveData.postValue(ProgressData(true))
        
        // Check if we need to initialize repository
        if (repository == null) initRepository(context)

        // ✅ OFFLINE SUPPORT: Check network status
        if (!NetworkUtils.isInternetAvailable(context)) {
            Log.d("OFFLINE_DEBUG", "Network unavailable, queuing dispatch request offline")
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val id = repository?.queueDispatchRequest(request) ?: -1L
                    loadingLiveData.postValue(ProgressData(false))
                    
                    if (id > 0) {
                        dispatchLiveData.postValue(DispatchResponse(true, "Dispatch queued offline successfully!", 200))
                    } else {
                        dispatchLiveData.postValue(DispatchResponse(false, "Failed to queue dispatch offline", 500))
                    }
                } catch (e: Exception) {
                    loadingLiveData.postValue(ProgressData(false))
                    dispatchLiveData.postValue(DispatchResponse(false, "Offline error: ${e.message}", 500))
                }
            }
            return
        }

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
