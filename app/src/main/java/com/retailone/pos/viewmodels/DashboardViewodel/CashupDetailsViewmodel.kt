package com.retailone.pos.viewmodels.DashboardViewodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.retailone.pos.models.CashupModel.CashupDetails.CashupDetailsReq
import com.retailone.pos.models.CashupModel.CashupDetails.CashupDetailsRes
import com.retailone.pos.models.CashupModel.CashupSubmit.CashupSubmitReq
import com.retailone.pos.models.CashupModel.CashupSubmit.CashupSubmitRes
import com.retailone.pos.models.CashupModel.SendOTP.SendOtpReq
import com.retailone.pos.models.CashupModel.SendOTP.SendOtpRes
import com.retailone.pos.models.CashupModel.VerifyOTP.VerifyOtpReq
import com.retailone.pos.models.CashupModel.VerifyOTP.VerifyOtpRes

import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CashupDetailsViewmodel : ViewModel() {

    val cashupdetails_data = MutableLiveData<CashupDetailsRes>()
    val cashupdetails_liveData: LiveData<CashupDetailsRes>
    get() = cashupdetails_data

    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData : LiveData<ProgressData>
    get() = loading

    val sendotp_data = MutableLiveData<SendOtpRes>()
    val sendotp_liveData: LiveData<SendOtpRes>
        get() = sendotp_data

    val verifyotp_data = MutableLiveData<VerifyOtpRes>()
    val verifyotp_liveData: LiveData<VerifyOtpRes>
        get() = verifyotp_data

    val cashupsubmit_data = MutableLiveData<CashupSubmitRes>()
    val cashupsubmit_liveData: LiveData<CashupSubmitRes>
        get() = cashupsubmit_data

    fun callcashupDetailsApi(cashupDetailsReq: CashupDetailsReq, context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).getCashupDetailsAPI(cashupDetailsReq).enqueue(object :
            Callback<CashupDetailsRes> {
            override fun onResponse(call: Call<CashupDetailsRes>, response: Response<CashupDetailsRes>) {

                if(response.isSuccessful && response.body()!=null){
                    cashupdetails_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<CashupDetailsRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }

    fun callSendBankOtpApi(sendOtpReq: SendOtpReq, context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).getSendOtpBankAPI(sendOtpReq).enqueue(object :
            Callback<SendOtpRes> {
            override fun onResponse(call: Call<SendOtpRes>, response: Response<SendOtpRes>) {

               /*val  gson:Gson = Gson();
                Log.d("gsonX",gson.toJson(response))*/

                if(response.isSuccessful && response.body()!=null){
                    sendotp_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<SendOtpRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }

    fun callVerifyOtpApi(verifyOtpReq: VerifyOtpReq, context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).getVerifyOtpBankAPI(verifyOtpReq).enqueue(object :
            Callback<VerifyOtpRes> {
            override fun onResponse(call: Call<VerifyOtpRes>, response: Response<VerifyOtpRes>) {

                if(response.isSuccessful && response.body()!=null){
                    verifyotp_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<VerifyOtpRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }


    fun callSubmitCashupApi(cashupSubmitReq: CashupSubmitReq, context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).getCashupSubmitAPI(cashupSubmitReq).enqueue(object :
            Callback<CashupSubmitRes> {
            override fun onResponse(call: Call<CashupSubmitRes>, response: Response<CashupSubmitRes>) {

                if(response.isSuccessful && response.body()!=null){
                    cashupsubmit_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<CashupSubmitRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }




}