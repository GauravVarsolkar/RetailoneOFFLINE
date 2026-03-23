package com.retailone.pos.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.retailone.pos.models.ChangePinModel.ChangePinRequest
import com.retailone.pos.models.ChangePinModel.ChangePinResponse
import com.retailone.pos.models.LoginModels.LoginRequest
import com.retailone.pos.models.LoginModels.LoginResponse
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.models.SendOTPModel.SendOtpRequest
import com.retailone.pos.models.SendOTPModel.SendOtpResponse
import com.retailone.pos.models.VerifyOtpModel.VerifyOtpRequest
import com.retailone.pos.models.VerifyOtpModel.VerifyOtpResponse
import com.retailone.pos.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPinViewmodel:ViewModel() {

    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData : LiveData<ProgressData>
        get() = loading


    val otpdata = MutableLiveData<SendOtpResponse>()
    val otpLivedata : LiveData<SendOtpResponse>
        get() = otpdata

    val verifydata = MutableLiveData<VerifyOtpResponse>()
    val verifyLivedata : LiveData<VerifyOtpResponse>
        get() = verifydata

    val changepindata = MutableLiveData<ChangePinResponse>()
    val changepinLivedata : LiveData<ChangePinResponse>
        get() = changepindata



    fun callChangePinApi( mobile: String,pin:String){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiServiceNoToken().changePIN(ChangePinRequest(mobile,pin)).enqueue(object :
            Callback<ChangePinResponse> {
            override fun onResponse(call: Call<ChangePinResponse>, response: Response<ChangePinResponse>) {

                if(response.isSuccessful && response.body()!=null){
                    changepindata.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<ChangePinResponse>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }


    fun callVerifyOtpApi( mobile: String,otp:String){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiServiceNoToken().verifyOTP(VerifyOtpRequest(mobile,otp)).enqueue(object :
            Callback<VerifyOtpResponse> {
            override fun onResponse(call: Call<VerifyOtpResponse>, response: Response<VerifyOtpResponse>) {

                if(response.isSuccessful && response.body()!=null){
                    verifydata.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<VerifyOtpResponse>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }

    fun callSendOtpApi(context: Context, mobile: String){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiServiceNoToken().sendOTP(SendOtpRequest(mobile)).enqueue(object :
            Callback<SendOtpResponse> {
            override fun onResponse(call: Call<SendOtpResponse>, response: Response<SendOtpResponse>) {

                if(response.isSuccessful && response.body()!=null){
                    otpdata.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<SendOtpResponse>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }

}
