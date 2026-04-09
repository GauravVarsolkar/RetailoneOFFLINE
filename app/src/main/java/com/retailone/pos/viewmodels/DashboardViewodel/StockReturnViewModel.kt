package com.retailone.pos.viewmodels.DashboardViewodel

import StockReturnResponse
import StockReturn
import ReturnedProduct
import Product             // ✅ ADDED
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.retailone.pos.network.ApiClient
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StockReturnViewModel : ViewModel() {
    private val _stockReturns = MutableLiveData<StockReturnResponse>()
    val stockReturns: LiveData<StockReturnResponse> get() = _stockReturns

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private var stockReturnDao: com.retailone.pos.localstorage.RoomDB.StockReturnDao? = null
    private var pendingGoodsReturnDao: com.retailone.pos.localstorage.RoomDB.PendingGoodsReturnDao? = null
    private var pendingDispatchDao: com.retailone.pos.localstorage.RoomDB.PendingDispatchDao? = null

    fun initRepository(context: Context) {
        val database = com.retailone.pos.localstorage.RoomDB.PosDatabase.getDatabase(context)
        stockReturnDao = database.stockReturnDao()
        pendingGoodsReturnDao = database.pendingGoodsReturnDao()
        pendingDispatchDao = database.pendingDispatchDao()
    }

    // ✅ MERGE: Offline pending data + Cached server data
    suspend fun getMergedReturns(storeId: Int): StockReturnResponse {
        // 1. Get cached server returns
        val entity = stockReturnDao?.getStockReturnListByStore(storeId)
        val cachedResponse = entity?.let {
            Gson().fromJson(it.stock_return_json, StockReturnResponse::class.java)
        } ?: StockReturnResponse(data = emptyList())

        // 2. Calculate offset for offline IDs to show a sequence (e.g., #304 -> #305)
        val maxServerId = cachedResponse.data.maxByOrNull { it.id }?.id ?: 0
        
        // 3. Get pending offline returns from local DB
        val pendingEntities = pendingGoodsReturnDao?.getAllPendingReturns() ?: emptyList()
        val pendingReturns = pendingEntities.map { pending: com.retailone.pos.localstorage.RoomDB.PendingGoodsReturnEntity ->
            val request = Gson().fromJson(pending.return_request_json, com.retailone.pos.models.GoodsToWarehouseModel.Stock.StockReturnRequests::class.java)
            
            // Map StockReturnRequests to StockReturn UI Model
            // Use temporary ID = maxServerId + localId to show sequence
            val tempId = if (pending.id < 1000) maxServerId + pending.id else pending.id

            StockReturn(
                id = tempId,
                status = 1, // Pending
                requested_date = pending.return_date,
                products = request.items.map { item: com.retailone.pos.models.GoodsToWarehouseModel.Stock.StockReturnItem ->
                    ReturnedProduct(
                        id = 0,
                        stock_return_id = tempId,
                        product_id = item.product_id,
                        quantity = item.quantity,
                        current_stock = 0,
                        approved_quantity = 0,
                        received_quantity = 0,
                        remarks = pending.remarks,
                        created_at = pending.return_date,
                        updated_at = pending.return_date,
                        condition = "",
                        rejected = 0,
                        seal_no = "",
                        remark = "",
                        product = Product(
                            id = item.product_id,
                            product_name = item.product_name,
                            product_description = "",
                            type = "",
                            category_id = 0,
                            tax_id = 0,
                            photo = null,
                            photo_name = null,
                            deleted_at = null,
                            created_at = null,
                            updated_at = null,
                            status = 1
                        )
                    )
                }
            )
        }

        // 4. Get pending dispatches to override status for server-side approved returns
        val pendingDispatches = pendingDispatchDao?.getAllPendingDispatches() ?: emptyList()
        val dispatchMap = pendingDispatches.associateBy { it.return_id }

        // 5. Combine both lists and apply status overrides
        val combinedList = (pendingReturns + cachedResponse.data).map { item ->
            val pendingDispatch = dispatchMap[item.id]
            if (pendingDispatch != null) {
                // Override status to "Dispatched" (4)
                item.copy(status = 4)
            } else {
                item
            }
        }.distinctBy { it.id } // simple deduplication
            .sortedByDescending { it.id }
        
        return StockReturnResponse(data = combinedList)
    }

    suspend fun getStockReturnsFromCache(storeId: Int): StockReturnResponse? {
        return getMergedReturns(storeId)
    }

    fun fetchStockReturns(context: Context) {
        _loading.postValue(true)

        ApiClient().getApiService(context).getStockReturns()
            .enqueue(object : Callback<StockReturnResponse> {
                override fun onResponse(
                    call: Call<StockReturnResponse>,
                    response: Response<StockReturnResponse>
                ) {
                    _loading.postValue(false)

                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        
                        GlobalScope.launch(Dispatchers.Main) {
                            try {
                                val loginSession = com.retailone.pos.localstorage.DataStore.LoginSession.getInstance(context)
                                val storeId = loginSession.getStoreID().first().toInt()
                                
                                // Cache result in background
                                withContext(Dispatchers.IO) {
                                    stockReturnDao?.insertStockReturnList(
                                        com.retailone.pos.localstorage.RoomDB.StockReturnEntity(
                                            store_id = storeId,
                                            stock_return_json = Gson().toJson(body)
                                        )
                                    )
                                }
                                
                                val merged = getMergedReturns(storeId)
                                _stockReturns.postValue(merged)
                            } catch (e: Exception) {
                                Log.e("StockReturnsAPI", "Error merging: ${e.message}")
                                _stockReturns.postValue(body)
                            }
                        }
                    } else {
                        Log.e("StockReturnsAPI", "Error body: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<StockReturnResponse>, t: Throwable) {
                    _loading.postValue(false)
                    Log.e("StockReturnsAPI", "API failed: ${t.message}", t)
                }
            })
    }
}
