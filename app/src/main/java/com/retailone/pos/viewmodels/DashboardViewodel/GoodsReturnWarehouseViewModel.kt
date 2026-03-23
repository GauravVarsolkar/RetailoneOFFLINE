package com.retailone.pos.viewmodels.DashboardViewodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.retailone.pos.models.GoodsToWarehouseModel.Stock.StockReturnRequests
import com.retailone.pos.models.GoodsToWarehouseModel.Stock.StockReturnResponses
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GoodsReturnWarehouseViewModel: ViewModel() {
    val stockListLiveData = MutableLiveData<StockReturnResponses>()
    val loadingLiveData = MutableLiveData<ProgressData>()
    val stockReturnSubmitLiveData = MutableLiveData<StockReturnResponses>()

    fun submitStockReturn(request: StockReturnRequests, context: Context) {
        loadingLiveData.postValue(ProgressData(true))

        ApiClient().getApiService(context).submitStockReturn(request).enqueue(object: Callback<StockReturnResponses> {
            override fun onResponse(call: Call<StockReturnResponses>, response: Response<StockReturnResponses>) {
                loadingLiveData.postValue(ProgressData(false))
                if (response.isSuccessful && response.body() != null) {
                    stockReturnSubmitLiveData.postValue(response.body())
                } else {
                    stockReturnSubmitLiveData.postValue(
                        StockReturnResponses("error", "Submit failed: ${response.message()}", errors = null)
                    )
                }
            }

            override fun onFailure(call: Call<StockReturnResponses>, t: Throwable) {
                loadingLiveData.postValue(ProgressData(false))
                stockReturnSubmitLiveData.postValue(
                    StockReturnResponses("error", "Error: ${t.localizedMessage}", errors = null)
                )
            }
        })
    }
}