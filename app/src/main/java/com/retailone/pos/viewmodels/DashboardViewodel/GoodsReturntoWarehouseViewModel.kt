package com.retailone.pos.viewmodels.DashboardViewodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.retailone.pos.models.GoodsToWarehouseModel.Stock.StockReturnRequests
import com.retailone.pos.models.GoodsToWarehouseModel.Stock.StockReturnResponses
import com.retailone.pos.models.GoodsToWarehouseModel.Stocklist.StockListResponse
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.models.ReturnSalesItemModel.SalesReturnReasonModel.SalesReturnReasonRes
import com.retailone.pos.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GoodsReturntoWarehouseViewModel: ViewModel() {
    val stockListLiveData = MutableLiveData<StockListResponse>()
    val loadingLiveData = MutableLiveData<ProgressData>()
    val stockListLiveDatas = MutableLiveData<StockReturnResponses>()
    val loadingLiveDatas = MutableLiveData<ProgressData>()
    val stockReturnSubmitLiveData = MutableLiveData<StockReturnResponses>()

    val salesreturnreason_data = MutableLiveData<SalesReturnReasonRes>()
    val salesreturnreason_liveData: LiveData<SalesReturnReasonRes>
        get() = salesreturnreason_data

    val loading = MutableLiveData<ProgressData>()
    val loadingsLiveData : LiveData<ProgressData>
        get() = loading

    fun submitStockReturn(request: StockReturnRequests, context: Context) {
        loadingLiveData.postValue(ProgressData(true))
        val gson = Gson()
        Log.d("SubmitRequestJSONstock", gson.toJson(request))
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


    fun callStockListApi(storeId: String, context: Context) {
        loadingLiveData.postValue(ProgressData(isProgress = true))

        val request = mapOf("store_id" to storeId.toInt())


        // ✅ Print the request data
        Log.d("StockAPI", "Request Body: $request")

        ApiClient().getApiService(context).getStockListAPI(request).enqueue(object :
            Callback<StockListResponse> {
            override fun onResponse(
                call: Call<StockListResponse>,
                response: Response<StockListResponse>
            ) {
                // ✅ Log full raw response
                Log.d("StockAPI", "Raw Response: ${response.raw()}")
                Log.d("StockAPI", "Response Code: ${response.code()}")
                Log.d("StockAPI", "Response Body: ${response.body()}")
                loadingLiveData.postValue(ProgressData(isProgress = false))
                if (response.isSuccessful && response.body() != null) {
                    stockListLiveData.postValue(response.body())

                } else {
                    loadingLiveData.postValue(
                        ProgressData(
                            isProgress = false,
                            isMessage = true,
                            message = "Failed to fetch stock list, Try again"
                        )
                    )
                }
            }

            override fun onFailure(call: Call<StockListResponse>, t: Throwable) {
                loadingLiveData.postValue(ProgressData(false))
                loadingLiveData.postValue(
                    ProgressData(
                        isProgress = false,
                        isMessage = true,
                        message = "Something went wrong: ${t.message}"
                    )
                )
            }
        })
    }

    fun callSaleReturnReasonApi( context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).getReturnReasonAPI().enqueue(object :
            Callback<SalesReturnReasonRes> {
            override fun onResponse(call: Call<SalesReturnReasonRes>, response: Response<SalesReturnReasonRes>) {

                if(response.isSuccessful && response.body()!=null){
                    salesreturnreason_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false,))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<SalesReturnReasonRes>, t: Throwable) {
                Log.d("rty",t.message.toString())
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }


}