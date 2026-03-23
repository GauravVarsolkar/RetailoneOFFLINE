package com.retailone.pos.viewmodels.DashboardViewodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.retailone.pos.models.BarcodeModel.StockSearchBarcodeModel.StockSearchBarcodeReq
import com.retailone.pos.models.BarcodeModel.StockSearchBarcodeModel.StockSearchBarcodeRes
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.models.StockRequisitionModel.StockSearchModel.StockSearchReq
import com.retailone.pos.models.StockRequisitionModel.StockSearchModel.StockSearchRes
import com.retailone.pos.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StockRequsitionSearchViewmodel: ViewModel() {


    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData : LiveData<ProgressData>
        get() = loading


    val stockSearchdata = MutableLiveData<StockSearchRes>()
    val stockSearchLivedata : LiveData<StockSearchRes>
        get() = stockSearchdata


    val stockSearchBarcodedata = MutableLiveData<StockSearchBarcodeRes>()
    val stockSearchBarcodeLivedata : LiveData<StockSearchBarcodeRes>
        get() = stockSearchBarcodedata



    fun callStockSearchApi( searchname: String,storeid:String,context: Context){
        loading.postValue(ProgressData(isProgress = true))
        val gson = Gson()
        Log.d("SubmitSearchReqstock", gson.toJson(StockSearchReq(searchname,storeid)))
        ApiClient().getApiService(context).stockReqSearch(StockSearchReq(searchname,storeid)).enqueue(object :
            Callback<StockSearchRes> {
            override fun onResponse(call: Call<StockSearchRes>, response: Response<StockSearchRes>) {
                Log.d("SubmitSearchResstock", response.body().toString())
                try {
                    if (response.isSuccessful && response.body() != null) {
                        Log.d("SubmitSearchReq", "sucesss")
                        stockSearchdata.postValue(response.body())
                        loading.postValue(ProgressData(isProgress = false))
                    } else {
                        Log.d("SubmitSearchReq", "fail")
                        Log.e("API_ERROR", "Error body: ${response.errorBody()?.string()}")
                        loading.postValue(
                            ProgressData(
                                isProgress = false,
                                isMessage = true,
                                message = "Failed to fetch data, Try again"
                            )
                        )
                    }
                }catch (e: Exception){
                    Log.e("API_ERROR", "Exception parsing response", e)
                    loading.postValue(ProgressData(isProgress = false, isMessage = true, message = "Parsing error: ${e.localizedMessage}"))

                }
            }

            override fun onFailure(call: Call<StockSearchRes>, t: Throwable) {
                Log.e("API_ERROR on failure", "Exception parsing response", t)
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went  Yes Wrong"))
            }
        })
    }


    fun callStockSearchBarcodeApi( searchname: String,storeid: String,context: Context){
        loading.postValue(ProgressData(isProgress = true))
        val gson = Gson()
        Log.d("SubmitReqsearch", gson.toJson(StockSearchBarcodeReq(searchname,storeid)))

                ApiClient().getApiService(context).stockReqSearchBarcode(StockSearchBarcodeReq(searchname,storeid)).enqueue(object :
            Callback<StockSearchBarcodeRes> {
            override fun onResponse(call: Call<StockSearchBarcodeRes>, response: Response<StockSearchBarcodeRes>) {

                if(response.isSuccessful && response.body()!=null){
                    stockSearchBarcodedata.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<StockSearchBarcodeRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }



}