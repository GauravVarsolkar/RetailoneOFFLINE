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
import com.retailone.pos.localstorage.RoomDB.PendingGoodsReturnRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
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

    private var repository: PendingGoodsReturnRepository? = null
    private var stockListDao: com.retailone.pos.localstorage.RoomDB.StockListDao? = null
    private var returnReasonDao: com.retailone.pos.localstorage.RoomDB.ReturnReasonDao? = null

    fun initRepository(context: Context) {
        val database = com.retailone.pos.localstorage.RoomDB.PosDatabase.getDatabase(context)
        if (repository == null) {
            repository = PendingGoodsReturnRepository(database.pendingGoodsReturnDao())
        }
        stockListDao = database.stockListDao()
        returnReasonDao = database.returnReasonDao()
    }

    suspend fun queueReturnRequest(request: StockReturnRequests): Long {
        return repository?.queueReturnRequest(request) ?: -1L
    }

    suspend fun syncPendingReturns(context: Context): Boolean {
        return repository?.syncAllPendingReturns(context) ?: true
    }

    suspend fun getPendingCount(): Int {
        return repository?.getPendingCount() ?: 0
    }

    // ✅ NEW: Load Stock List from Cache
    suspend fun getStockListFromCache(storeId: Int): StockListResponse? {
        val entity = stockListDao?.getStockListByStore(storeId)
        return entity?.let {
            Gson().fromJson(it.stock_list_json, StockListResponse::class.java)
        }
    }

    // ✅ NEW: Load Return Reasons from Cache
    suspend fun getReturnReasonsFromCache(): SalesReturnReasonRes? {
        val entities = returnReasonDao?.getAllReasons() ?: emptyList()
        if (entities.isEmpty()) return null
        
        val reasons = entities.map {
            Gson().fromJson(it.reason_data_json, com.retailone.pos.models.ReturnSalesItemModel.SalesReturnReasonModel.ReturnReasonData::class.java)
        }
        return SalesReturnReasonRes(status = 1, message = "Success (cached)", data = reasons)
    }

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

        if (!com.retailone.pos.utils.NetworkUtils.isInternetAvailable(context)) {
            Log.d("StockAPI", "Offline mode: Skipping stock list API call")
            loadingLiveData.postValue(ProgressData(
                isProgress = false,
                isMessage = true,
                message = "Working Offline: Displaying cached stock data"
            ))
            return
        }

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
                    val body = response.body()!!
                    stockListLiveData.postValue(body)
                    
                    // ✅ Cache result in background
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            stockListDao?.insertStockList(
                                com.retailone.pos.localstorage.RoomDB.StockListEntity(
                                    store_id = storeId.toInt(),
                                    stock_list_json = Gson().toJson(body)
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("StockAPI", "Failed to cache stock list: ${e.message}")
                        }
                    }

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

        if (!com.retailone.pos.utils.NetworkUtils.isInternetAvailable(context)) {
            Log.d("ReasonAPI", "Offline mode: Skipping return reasons API call")
            loading.postValue(ProgressData(
                isProgress = false,
                isMessage = true,
                message = "Working Offline: Displaying cached return reasons"
            ))
            return
        }

        ApiClient().getApiService(context).getReturnReasonAPI().enqueue(object :
            Callback<SalesReturnReasonRes> {
            override fun onResponse(call: Call<SalesReturnReasonRes>, response: Response<SalesReturnReasonRes>) {

                if(response.isSuccessful && response.body()!=null){
                    val body = response.body()!!
                    salesreturnreason_data.postValue(body)
                    loading.postValue(ProgressData(isProgress = false,))
                    
                    // ✅ Cache result in background
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            val entities = body.data.map {
                                com.retailone.pos.localstorage.RoomDB.ReturnReasonEntity(
                                    id = it.id,
                                    reason_name = it.reason_name,
                                    reason_data_json = Gson().toJson(it)
                                )
                            }
                            returnReasonDao?.insertReasons(entities)
                        } catch (e: Exception) {
                            Log.e("ReasonAPI", "Failed to cache return reasons: ${e.message}")
                        }
                    }
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