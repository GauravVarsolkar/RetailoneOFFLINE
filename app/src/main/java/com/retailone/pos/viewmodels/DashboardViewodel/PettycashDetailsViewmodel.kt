package com.retailone.pos.viewmodels.DashboardViewodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.retailone.pos.models.PettycashReportModel.PettycashReportRes
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.InvoiceReq
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.InvoiceRes
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.SalesDetailsReq
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.SalesDetailsRes
import com.retailone.pos.models.SalesPaymentModel.SalesList.SalesListReq
import com.retailone.pos.models.SalesPaymentModel.SalesList.SalesListRes
import com.retailone.pos.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class PettycashDetailsViewmodel: ViewModel() {

    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData : LiveData<ProgressData>
        get() = loading

    val pettycashReport_data = MutableLiveData<PettycashReportRes>()
    val pettycashReport_liveData: LiveData<PettycashReportRes>
        get() = pettycashReport_data

    val pettycash_data = MutableLiveData<PettycashReportRes>()
    val pettycash_liveData: LiveData<PettycashReportRes>
        get() = pettycash_data


    fun callPettycashReportListApi(salesListReq: SalesListReq, context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).getPettyCashReport().enqueue(object :
            Callback<PettycashReportRes> {
            override fun onResponse(call: Call<PettycashReportRes>, response: Response<PettycashReportRes>) {

                if(response.isSuccessful && response.body()!=null){
                    pettycashReport_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<PettycashReportRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }


    fun callPettycashDataApi(store_id: String, context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).getPettyCashData(storeId = store_id).enqueue(object :
            Callback<PettycashReportRes> {
            override fun onResponse(call: Call<PettycashReportRes>, response: Response<PettycashReportRes>) {

                if(response.isSuccessful && response.body()!=null){
                    pettycash_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<PettycashReportRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }




}