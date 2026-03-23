package com.retailone.pos.viewmodels.DashboardViewodel

import StockReturnResponse
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.retailone.pos.network.ApiClient

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StockReturnViewModel : ViewModel() {
    private val _stockReturns = MutableLiveData<StockReturnResponse>()
    val stockReturns: LiveData<StockReturnResponse> get() = _stockReturns

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun fetchStockReturns(context: Context) {
        _loading.postValue(true)

        // 🔹 What we are sending
        // Since getStockReturns() has no @Body/@Query params,
        // it’s just a plain GET call with URL and headers.
        Log.d("StockReturnsAPI", "Request -> calling getStockReturns() (no body, only URL + headers)")

        ApiClient().getApiService(context).getStockReturns()
            .enqueue(object : Callback<StockReturnResponse> {
                override fun onResponse(
                    call: Call<StockReturnResponse>,
                    response: Response<StockReturnResponse>
                ) {
                    _loading.postValue(false)

                    Log.d("StockReturnsAPI", "Response code: ${response.code()}")

                    if (response.isSuccessful) {
                        val body = response.body()
                        // 🔹 Pretty print response body
                        Log.d("StockReturnsAPI", "Response body: ${Gson().toJson(body)}")
                        _stockReturns.postValue(body)
                    } else {
                        Log.e(
                            "StockReturnsAPI",
                            "Error body: ${response.errorBody()?.string()}"
                        )
                    }
                }

                override fun onFailure(call: Call<StockReturnResponse>, t: Throwable) {
                    _loading.postValue(false)
                    Log.e("StockReturnsAPI", "API failed: ${t.message}", t)
                }
            })
    }
}
