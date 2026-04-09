package com.retailone.pos.viewmodels.DashboardViewodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseCategory.ExpenseCategoryRes
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseHistory.ExpenseHistoryReq
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseHistory.ExpenseHistoryRes
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseImage.ExpenceImageRes
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseSubmit.ExpenseSubmitReq
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseSubmit.ExpenseSubmitRes
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseVendor.ExpenseVendorRes
import com.retailone.pos.models.LocalizationModel.LocalizationRes
import com.retailone.pos.models.ProductInventoryModel.InventoryUpdateReqModel.InventoryUpdateRequest
import com.retailone.pos.models.ProductInventoryModel.InventoryUpdateResModel.InventoryUpdateResponse
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.network.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import androidx.lifecycle.viewModelScope
import com.retailone.pos.localstorage.SharedPreference.ExpenseCacheHelper
import com.retailone.pos.repository.ExpenseRepository
import com.retailone.pos.utils.NetworkUtils
import kotlinx.coroutines.launch

class ExpenseRegisterViewmodel : ViewModel() {

    private lateinit var repository: ExpenseRepository
    private lateinit var cacheHelper: ExpenseCacheHelper

    private fun init(context: Context) {
        if (!::repository.isInitialized) {
            repository = ExpenseRepository(context)
            cacheHelper = ExpenseCacheHelper(context)
        }
    }

    val expensecategory_data = MutableLiveData<ExpenseCategoryRes>()
    val expensecategory_liveData: LiveData<ExpenseCategoryRes>
        get() = expensecategory_data

    val expensevendor_data = MutableLiveData<ExpenseVendorRes>()
    val expensevendor_liveData: LiveData<ExpenseVendorRes>
        get() = expensevendor_data

    val expensesubmit_data = MutableLiveData<ExpenseSubmitRes>()
    val expensesubmit_liveData: LiveData<ExpenseSubmitRes>
        get() = expensesubmit_data

    val expensehistory_data = MutableLiveData<ExpenseHistoryRes>()
    val expensehistory_liveData: LiveData<ExpenseHistoryRes>
        get() = expensehistory_data

    val invoiceupload_data = MutableLiveData<ExpenceImageRes>()
    val invoiceupload_liveData: LiveData<ExpenceImageRes>
        get() = invoiceupload_data

    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData : LiveData<ProgressData>
        get() = loading


    fun callExpenseSubmitApi(expenseSubmitReq: ExpenseSubmitReq, context: Context){
        init(context)
        loading.postValue(ProgressData(isProgress = true))

        viewModelScope.launch {
            repository.submitExpense(
                expenseSubmitReq,
                onSuccess = {
                    expensesubmit_data.postValue(it)
                    loading.postValue(ProgressData(isProgress = false))
                },
                onError = {
                    loading.postValue(ProgressData(isProgress = false, isMessage = true, message = it))
                }
            )
        }
    }


    fun callExpenseHistoryApi(expenseHistoryReq: ExpenseHistoryReq,context: Context) {
        init(context)
        
        // Load from cache immediately if offline
        if (!NetworkUtils.isInternetAvailable(context)) {
            val cached = cacheHelper.getHistory()
            if (cached != null) {
                expensehistory_data.postValue(cached)
                return
            }
        }

        loading.postValue(ProgressData(isProgress = true))
        ApiClient().getApiService(context).getExpenceHistoryAPI(expenseHistoryReq)
            .enqueue(object : Callback<ExpenseHistoryRes> {
                override fun onResponse(
                    call: Call<ExpenseHistoryRes>,
                    response: Response<ExpenseHistoryRes>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        expensehistory_data.postValue(body)
                        cacheHelper.saveHistory(body) // Cache history
                        loading.postValue(ProgressData(isProgress = false))
                    } else {
                        // Fallback to cache
                        val cached = cacheHelper.getHistory()
                        if (cached != null) {
                            expensehistory_data.postValue(cached)
                            loading.postValue(ProgressData(isProgress = false))
                        } else {
                            loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch history, Try again" ))
                        }
                    }
                }

                override fun onFailure(call: Call<ExpenseHistoryRes>, t: Throwable) {
                    val cached = cacheHelper.getHistory()
                    if (cached != null) {
                        expensehistory_data.postValue(cached)
                        loading.postValue(ProgressData(isProgress = false))
                    } else {
                        loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
                    }
                }
            })
    }


    fun callExpenseCategoryApi(context: Context) {
        init(context)
        
        // Show cached data immediately if offline
        if (!NetworkUtils.isInternetAvailable(context)) {
            val cached = cacheHelper.getCategories()
            if (cached != null) {
                expensecategory_data.postValue(cached)
                return
            }
        }
        
        loading.postValue(ProgressData(isProgress = true))
        ApiClient().getApiService(context).getExpenceCategoryAPI()
            .enqueue(object : Callback<ExpenseCategoryRes> {
                override fun onResponse(
                    call: Call<ExpenseCategoryRes>,
                    response: Response<ExpenseCategoryRes>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        expensecategory_data.postValue(response.body())
                        cacheHelper.saveCategories(response.body()!!)
                        loading.postValue(ProgressData(isProgress = false))
                    } else {
                        // Fallback to cache if API fails
                        val cached = cacheHelper.getCategories()
                        if (cached != null) {
                            expensecategory_data.postValue(cached)
                            loading.postValue(ProgressData(isProgress = false))
                        } else {
                            loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                        }
                    }
                }

                override fun onFailure(call: Call<ExpenseCategoryRes>, t: Throwable) {
                     val cached = cacheHelper.getCategories()
                     if (cached != null) {
                         expensecategory_data.postValue(cached)
                         loading.postValue(ProgressData(isProgress = false))
                     } else {
                         loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
                     }
                }
            })
    }

    fun callExpenseVendorApi(context: Context) {
        init(context)

        // Show cached data immediately if offline
        if (!NetworkUtils.isInternetAvailable(context)) {
            val cached = cacheHelper.getVendors()
            if (cached != null) {
                expensevendor_data.postValue(cached)
                return
            }
        }

        loading.postValue(ProgressData(isProgress = true))
        ApiClient().getApiService(context).getExpenceVendorAPI()
            .enqueue(object : Callback<ExpenseVendorRes> {
                override fun onResponse(
                    call: Call<ExpenseVendorRes>,
                    response: Response<ExpenseVendorRes>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        expensevendor_data.postValue(response.body())
                        cacheHelper.saveVendors(response.body()!!)
                        loading.postValue(ProgressData(isProgress = false))
                    } else {
                        // Fallback to cache if API fails
                        val cached = cacheHelper.getVendors()
                        if (cached != null) {
                            expensevendor_data.postValue(cached)
                            loading.postValue(ProgressData(isProgress = false))
                        } else {
                            loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                        }
                    }
                }

                override fun onFailure(call: Call<ExpenseVendorRes>, t: Throwable) {
                    val cached = cacheHelper.getVendors()
                    if (cached != null) {
                        expensevendor_data.postValue(cached)
                        loading.postValue(ProgressData(isProgress = false))
                    } else {
                        loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
                    }
                }
            })
    }

    fun callInvoiceUploadApi(filePart: MultipartBody.Part, context: Context) {

        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).uploadInvoice(filePart)
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