package com.retailone.pos.viewmodels.DashboardViewodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.retailone.pos.models.PiUpdateModel.PiUpdateData
import com.retailone.pos.models.PiUpdateModel.PiUpdateItem
import com.retailone.pos.models.ProductInventoryModel.InventoryUpdateReqModel.InventoryUpdateRequest
import com.retailone.pos.models.ProductInventoryModel.InventoryUpdateResModel.InventoryUpdateResponse
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PiUpdateViewmodel:ViewModel() {


    val pi_update_data = MutableLiveData<PiUpdateData>()

    val pi_update_LiveData : LiveData<PiUpdateData>
        get() = pi_update_data

    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData : LiveData<ProgressData>
        get() = loading

    val inventoryupdatedata = MutableLiveData<InventoryUpdateResponse>()
    val inventoryUpdateLiveData : LiveData<InventoryUpdateResponse>
        get() = inventoryupdatedata

    fun getPiUpdateData(){

        val list = arrayListOf<PiUpdateItem>()
        list.add(PiUpdateItem("MM Gold Premiun","asdfghj","1","num"))
        list.add(PiUpdateItem("MM Gold Premiun","asdfghj","1","num"))
        list.add(PiUpdateItem("Loose Oil","asdfghj","1","ml"))

        val data =  PiUpdateData(true,list)
        pi_update_data.postValue(data)


    }

    fun callInventoryUpdateApi(inventoryUpdateRequest: InventoryUpdateRequest,context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).updateInventoryStock(inventoryUpdateRequest).enqueue(object :
            Callback<InventoryUpdateResponse> {
            override fun onResponse(call: Call<InventoryUpdateResponse>, response: Response<InventoryUpdateResponse>) {

                if(response.isSuccessful && response.body()!=null){
                    inventoryupdatedata.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<InventoryUpdateResponse>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }

}