package com.retailone.pos.viewmodels.DashboardViewodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseImage.ExpenceImageRes
import com.retailone.pos.models.MaterialRcvModel.MaterialRcvInv.MatRcvInvReq
import com.retailone.pos.models.MaterialRcvModel.MaterialRcvInv.MatRcvInvRes
import com.retailone.pos.models.MaterialRcvModel.MaterialRcvItemtest
import com.retailone.pos.models.MaterialRcvModel.MaterialReceived
import com.retailone.pos.models.MaterialRcvModel.MaterialReceivedRes
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.network.ApiClient
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MaterialReceivingViewmodel: ViewModel() {

    val material_rcv = MutableLiveData<MaterialReceived>()

    val material_rcv_LiveData : LiveData<MaterialReceived>
        get() = material_rcv

    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData : LiveData<ProgressData>
        get() = loading


    val material_received_submit_data = MutableLiveData<MatRcvInvRes>()

    val material_received_submit_livedata : LiveData<MatRcvInvRes>
        get() = material_received_submit_data

    val invoiceupload_data = MutableLiveData<ExpenceImageRes>()
    val invoiceupload_liveData: LiveData<ExpenceImageRes>
        get() = invoiceupload_data



    fun callMaterialReceivedSubmitApi(materialReceivedReq: MatRcvInvReq, context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).sendReceivedMaterials(materialReceivedReq).enqueue(object :
            Callback<MatRcvInvRes> {
            override fun onResponse(call: Call<MatRcvInvRes>, response: Response<MatRcvInvRes>) {

                if(response.isSuccessful && response.body()!=null){
                    material_received_submit_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }
            override fun onFailure(call: Call<MatRcvInvRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }

    fun getMaterialRcvData(){

        val list = arrayListOf<MaterialRcvItemtest>()
        list.add(MaterialRcvItemtest("MM Gold Premiun","asdfghj","1","num"))
        list.add(MaterialRcvItemtest("MM Gold Premiun","asdfghj","1","num"))
        list.add(MaterialRcvItemtest("Loose Oil","asdfghj","1","ml"))

        val data =  MaterialReceived(true,list)
        material_rcv.postValue(data)

    }


    fun callSTNUploadApi(filePart: MultipartBody.Part, context: Context) {

        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).uploadSTNImage(filePart)
            .enqueue(object : Callback<ExpenceImageRes> {
                override fun onResponse(
                    call: Call<ExpenceImageRes>,
                    response: Response<ExpenceImageRes>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        invoiceupload_data.postValue(response.body())
                        loading.postValue(ProgressData(isProgress = false))
                    } else {
                        loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                    }
                }

                override fun onFailure(call: Call<ExpenceImageRes>, t: Throwable) {
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
                }
            })

    }
}