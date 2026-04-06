package com.retailone.pos.viewmodels.DashboardViewodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.PastReqDetailsReq
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.PastReqDetailsRes
import com.retailone.pos.models.StockRequisitionModel.PastRequsitionModel.PastRequsitionReq
import com.retailone.pos.models.StockRequisitionModel.PastRequsitionModel.PastRequsitionRes
import com.retailone.pos.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StockRequisitionViewmodel: ViewModel() {


    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData : LiveData<ProgressData>
        get() = loading


    val pastrequsition_data = MutableLiveData<PastRequsitionRes>()

    val pastrequsition_livedata : LiveData<PastRequsitionRes>
        get() = pastrequsition_data


    val past_req_details = MutableLiveData<PastReqDetailsRes>()

    val past_req_details_livedata : LiveData<PastReqDetailsRes>
        get() = past_req_details



    fun callPastRequsitionApi(store_id: String, context: Context){
        loading.postValue(ProgressData(isProgress = true))

        if (!com.retailone.pos.utils.NetworkUtils.isInternetAvailable(context)) {
            Log.d("StockReqAPI", "Offline mode: Skipping past requisition API call")
            loading.postValue(ProgressData(
                isProgress = false,
                isMessage = true,
                message = "Working Offline: Please check your network"
            ))
            return
        }

        ApiClient().getApiService(context).pastRequsition(PastRequsitionReq(store_id)).enqueue(object :
            Callback<PastRequsitionRes> {
            override fun onResponse(call: Call<PastRequsitionRes>, response: Response<PastRequsitionRes>) {

                if(response.isSuccessful && response.body()!=null){
                    pastrequsition_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false ))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<PastRequsitionRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }

    fun callRequisitionDetailsApi(request_id: String, context: Context){
        loading.postValue(ProgressData(isProgress = true))

        if (!com.retailone.pos.utils.NetworkUtils.isInternetAvailable(context)) {
            Log.d("StockReqAPI", "Offline mode: Skipping requisition details API call")
            loading.postValue(ProgressData(
                isProgress = false,
                isMessage = true,
                message = "Working Offline: Please check your network"
            ))
            return
        }

        ApiClient().getApiService(context).pastRequsitionDetails(PastReqDetailsReq(request_id)).enqueue(object :
            Callback<PastReqDetailsRes> {
            override fun onResponse(call: Call<PastReqDetailsRes>, response: Response<PastReqDetailsRes>) {

                if(response.isSuccessful && response.body()!=null){
                    past_req_details.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<PastReqDetailsRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }




}