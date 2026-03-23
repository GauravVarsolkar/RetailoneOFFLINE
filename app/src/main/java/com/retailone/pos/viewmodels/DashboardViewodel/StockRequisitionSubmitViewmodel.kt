package com.retailone.pos.viewmodels.DashboardViewodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.models.StockRequisitionModel.SubmitStockRequsitionModel.SubmitStockRequest
import com.retailone.pos.models.StockRequisitionModel.SubmitStockRequsitionModel.SubmitStockResponse
import com.retailone.pos.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StockRequisitionSubmitViewmodel :ViewModel() {


    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData : LiveData<ProgressData>
        get() = loading


    val stockreq_submit_data = MutableLiveData<SubmitStockResponse>()

    val stockreq_submit_livedata : LiveData<SubmitStockResponse>
        get() = stockreq_submit_data




    fun callStockReqSubmitApi( submitStockRequest: SubmitStockRequest,context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).submitStockRequsition(submitStockRequest).enqueue(object :
            Callback<SubmitStockResponse> {
            override fun onResponse(call: Call<SubmitStockResponse>, response: Response<SubmitStockResponse>) {

                if(response.isSuccessful && response.body()!=null){
                    stockreq_submit_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<SubmitStockResponse>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }



}