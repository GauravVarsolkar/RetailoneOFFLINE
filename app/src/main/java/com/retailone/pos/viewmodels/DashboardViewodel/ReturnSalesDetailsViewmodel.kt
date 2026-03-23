package com.retailone.pos.viewmodels.DashboardViewodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.models.ReplaceModel.ReplaceSaleReq
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemReq
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemRes
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel.ReturnSaleReq
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel.SalesListRequest
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel.ReturnSaleRes
import com.retailone.pos.models.ReturnSalesItemModel.SalesReturnReasonModel.SalesReturnReasonRes
import com.retailone.pos.models.SalesListResponse
import com.retailone.pos.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.retailone.pos.models.ReplaceModel.ReturnSaleResMapper
import com.retailone.pos.models.ReplaceModel.ReturnSaleResRaw
import com.retailone.pos.repository.CompletedSaleRepository
import com.retailone.pos.repository.DetailedSaleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.retailone.pos.models.SalesData
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemData
import com.retailone.pos.models.ReturnSalesItemModel.SalesReturnReasonModel.ReturnReasonData
import com.retailone.pos.repository.ReturnReasonRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.retailone.pos.repository.PendingReturnRepository
import com.retailone.pos.utils.NetworkUtils



class ReturnSalesDetailsViewmodel : ViewModel() {

    val returnitem_data = MutableLiveData<ReturnItemRes>()
    val returnitem_liveData: LiveData<ReturnItemRes>
        get() = returnitem_data

    private var completedSaleRepository: CompletedSaleRepository? = null
    private var detailedSaleRepository: DetailedSaleRepository? = null
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var returnReasonRepository: ReturnReasonRepository? = null
    private var pendingReturnRepository: PendingReturnRepository? = null



    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData: LiveData<ProgressData>
        get() = loading

    val returnsalesubmit_data = MutableLiveData<ReturnSaleRes>()
    val returnsalesubmit_liveData: LiveData<ReturnSaleRes>
        get() = returnsalesubmit_data

    val salesreturnreason_data = MutableLiveData<SalesReturnReasonRes>()
    val salesreturnreason_liveData: LiveData<SalesReturnReasonRes>
        get() = salesreturnreason_data

    val salesListLiveData = MutableLiveData<SalesListResponse>()

    // ✅ Initialize all repositories
    fun initRepository(context: Context) {
        if (completedSaleRepository == null) {
            completedSaleRepository = CompletedSaleRepository(context)
        }
        if (detailedSaleRepository == null) {
            detailedSaleRepository = DetailedSaleRepository(context)
        }
        if (returnReasonRepository == null) {
            returnReasonRepository = ReturnReasonRepository(context)
        }
        if (pendingReturnRepository == null) {
            pendingReturnRepository = PendingReturnRepository(context)
        }
    }

    // ✅ Get reason name by ID from local database
    suspend fun getReasonNameById(reasonId: Int): String {
        return withContext(Dispatchers.IO) {
            returnReasonRepository?.getReasonNameById(reasonId) ?: "Not Given"
        }
    }

    // ✅ Get sales from local database (offline-capable)
    suspend fun getSalesFromLocalDB(): List<SalesData> {
        return withContext(Dispatchers.IO) {
            try {
                completedSaleRepository?.let { repo ->
                    val entities = repo.getAllSalesFlow().first()
                    repo.entitiesToSalesDataList(entities)
                } ?: emptyList()
            } catch (e: Exception) {
                Log.e("ViewModel", "Error loading from local DB: ${e.message}")
                emptyList()
            }
        }
    }

    // ✅ Queue return request for offline submission
    suspend fun queueReturnRequest(returnRequest: ReturnSaleReq): Long {
        return withContext(Dispatchers.IO) {
            pendingReturnRepository?.queueReturnRequest(returnRequest) ?: -1L
        }
    }

    // ✅ Get pending returns count
    suspend fun getPendingReturnsCount(): Int {
        return withContext(Dispatchers.IO) {
            pendingReturnRepository?.getPendingReturnsCount() ?: 0
        }
    }

    // ✅ Sync all pending returns
    suspend fun syncPendingReturns(context: Context) {
        withContext(Dispatchers.IO) {
            val pendingReturns = pendingReturnRepository?.getAllPendingReturns() ?: emptyList()

            if (pendingReturns.isEmpty()) {
                Log.d("PendingReturns", "No pending returns to sync")
                return@withContext
            }

            Log.d("PendingReturns", "🔄 Syncing ${pendingReturns.size} pending returns...")

            pendingReturns.forEach { entity ->
                try {
                    // Update status to SYNCING
                    pendingReturnRepository?.updateSyncStatus(entity.id, "SYNCING")

                    // Convert back to ReturnSaleReq
                    val returnRequest = pendingReturnRepository?.entityToReturnRequest(entity)

                    if (returnRequest != null) {
                        // Call API synchronously
                        val response = ApiClient().getApiService(context)
                            .getReturnSalesSubmitAPI(returnRequest)
                            .execute()

                        if (response.isSuccessful && response.body()?.status == 1) {
                            // Mark as synced
                            pendingReturnRepository?.markAsSynced(entity.id)

                            // ✅ Update cached sale to mark as refunded with reason
                            try {
                                // Get invoice_id from the pending return entity
                                val invoiceId = entity.invoice_id

                                if (!invoiceId.isNullOrEmpty()) {
                                    val saleDetails = detailedSaleRepository?.getDetailedSaleByInvoiceId(invoiceId)

                                    if (saleDetails != null) {
                                        val grandTotal = saleDetails.grand_total.toDoubleOrNull() ?: 0.0
                                        val reasonId = returnRequest.reason_id ?: -1

                                        detailedSaleRepository?.updateRefundedAmount(
                                            invoiceId,
                                            grandTotal,
                                            reasonId
                                        )

                                        Log.d("PendingReturns", "✅ Updated cache for $invoiceId with reason $reasonId")
                                    } else {
                                        Log.w("PendingReturns", "⚠️ Sale not found in cache for invoice: $invoiceId")
                                    }
                                } else {
                                    Log.w("PendingReturns", "⚠️ No invoice_id in pending return entity")
                                }
                            } catch (e: Exception) {
                                Log.e("PendingReturns", "❌ Failed to update cache: ${e.message}", e)
                            }

                            Log.d("PendingReturns", "✅ Synced return ${entity.id}")
                        } else {
                            // Mark as failed
                            val errorMsg = response.body()?.message ?: "Sync failed"
                            pendingReturnRepository?.updateSyncStatus(entity.id, "FAILED", errorMsg)
                            Log.e("PendingReturns", "❌ Failed to sync return ${entity.id}: $errorMsg")
                        }
                    }
                } catch (e: Exception) {
                    // Mark as failed
                    pendingReturnRepository?.updateSyncStatus(entity.id, "FAILED", e.message)
                    Log.e("PendingReturns", "❌ Error syncing return ${entity.id}: ${e.message}")
                }
            }

            Log.d("PendingReturns", "🎉 Sync complete!")
        }
    }


    // ✅ Search sale by invoice ID from local database (offline)
    suspend fun searchSaleByInvoiceOffline(invoiceId: String): SalesData? {
        return withContext(Dispatchers.IO) {
            completedSaleRepository?.getSaleByInvoiceId(invoiceId)
        }
    }

    // ✅ UPDATED: Update sale's refunded amount AND reason_id after successful return
    suspend fun updateSaleRefundedAmount(invoiceId: String, refundedAmount: Double, reasonId: Int) {
        withContext(Dispatchers.IO) {
            detailedSaleRepository?.updateRefundedAmount(invoiceId, refundedAmount, reasonId)
        }
    }


    // ✅ Save sales to local database after API call
    private suspend fun saveSalesToLocalDB(salesList: List<SalesData>) {
        withContext(Dispatchers.IO) {
            completedSaleRepository?.saveSalesFromList(salesList)
        }
    }

    // ✅ Delete old sales (7+ days)
    suspend fun cleanupOldSales() {
        withContext(Dispatchers.IO) {
            completedSaleRepository?.deleteOldSales()
        }
    }

    // ✅ Save detailed sale to local DB (call after API success)
    suspend fun saveDetailedSaleToLocalDB(returnItemData: ReturnItemData) {
        withContext(Dispatchers.IO) {
            detailedSaleRepository?.saveDetailedSale(returnItemData)
        }
    }

    // ✅ Get detailed sale from local DB (for offline viewing)
    suspend fun getDetailedSaleFromLocalDB(invoiceId: String): ReturnItemData? {
        return withContext(Dispatchers.IO) {
            detailedSaleRepository?.getDetailedSaleByInvoiceId(invoiceId)
        }
    }

    // ✅ Check if detailed sale exists in local DB
    suspend fun detailedSaleExistsInDB(invoiceId: String): Boolean {
        return withContext(Dispatchers.IO) {
            detailedSaleRepository?.detailedSaleExists(invoiceId) ?: false
        }
    }

    // ✅ Save return reasons to local DB
    suspend fun saveReturnReasonsToLocalDB(reasons: List<ReturnReasonData>) {
        withContext(Dispatchers.IO) {
            returnReasonRepository?.saveReturnReasons(reasons)
        }
    }

    // ✅ Get return reasons from local DB
    suspend fun getReturnReasonsFromLocalDB(): List<ReturnReasonData> {
        return withContext(Dispatchers.IO) {
            returnReasonRepository?.getAllReturnReasons() ?: emptyList()
        }
    }

    // ✅ Check if return reasons exist in local DB
    suspend fun hasReturnReasonsInDB(): Boolean {
        return withContext(Dispatchers.IO) {
            returnReasonRepository?.hasReturnReasons() ?: false
        }
    }


    // ✅ Cleanup old detailed sales (7+ days)
    suspend fun cleanupOldDetailedSales() {
        withContext(Dispatchers.IO) {
            detailedSaleRepository?.deleteOldDetailedSales()
        }
    }

    private fun logLong(tag: String, msg: String) {
        if (msg.length <= 4000) {
            Log.d(tag, msg)
            return
        }
        var i = 0
        while (i < msg.length) {
            val end = (i + 4000).coerceAtMost(msg.length)
            Log.d(tag, msg.substring(i, end))
            i = end
        }
    }

    fun callSalesListApi(context: Context, storeId: String) {
        loading.postValue(ProgressData(isProgress = true))

        val request = SalesListRequest(store_id = storeId)

        val gsonPretty = GsonBuilder().setPrettyPrinting().create()
        logLong("SalesListAPIRequest", "days=7\nbody=\n${gsonPretty.toJson(request)}")

        ApiClient().getApiService(context).getSalesList(days = 7, request)
            .enqueue(object : Callback<SalesListResponse> {
                override fun onResponse(
                    call: Call<SalesListResponse>, response: Response<SalesListResponse>
                ) {
                    val code = response.code()
                    val headersStr = response.headers().toString()

                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        val json = gsonPretty.toJson(body)

                        logLong(
                            "SalesListAPIResponse",
                            "HTTP $code\nHeaders:\n$headersStr\nBody:\n$json"
                        )

                        salesListLiveData.postValue(body)

                        // ✅ Save to local database after successful API call
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            saveSalesToLocalDB(body.data)
                        }

                        loading.postValue(ProgressData(isProgress = false))
                    } else {
                        val err = try {
                            response.errorBody()?.string()
                        } catch (e: Exception) {
                            "errorBody read failed: ${e.message}"
                        } ?: "null"

                        loading.postValue(
                            ProgressData(
                                isProgress = false,
                                isMessage = true,
                                message = "Failed to fetch sales list, try again"
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<SalesListResponse>, t: Throwable) {
                    Log.e("SalesListAPI", "Error: ${t.localizedMessage}", t)
                    loading.postValue(
                        ProgressData(
                            isProgress = false,
                            isMessage = true,
                            message = "Something went wrong: ${t.message}"
                        )
                    )
                }
            })
    }

    fun callreplaceSalesListApi(context: Context, storeId: String) {
        loading.postValue(ProgressData(isProgress = true))

        val request = SalesListRequest(store_id = storeId)

        ApiClient().getApiService(context).getReplaceSalesList(days = 7, request)
            .enqueue(object : Callback<SalesListResponse> {
                override fun onResponse(
                    call: Call<SalesListResponse>, response: Response<SalesListResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        salesListLiveData.postValue(response.body())
                        loading.postValue(ProgressData(isProgress = false))
                    } else {
                        loading.postValue(
                            ProgressData(
                                isProgress = false,
                                isMessage = true,
                                message = "Failed to fetch sales list, try again"
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<SalesListResponse>, t: Throwable) {
                    Log.e("SalesListAPI", "Error: ${t.localizedMessage}")
                    loading.postValue(
                        ProgressData(
                            isProgress = false,
                            isMessage = true,
                            message = "Something went wrong: ${t.message}"
                        )
                    )
                }
            })
    }

    fun callReturnSalesDetailsApi(returnItemReq: ReturnItemReq, context: Context) {
        loading.postValue(ProgressData(isProgress = true))
        Log.e("SalesListAPIRequest", "Request: ${returnItemReq}")
        ApiClient().getApiService(context).getReturnSalesItemAPI(returnItemReq)
            .enqueue(object : Callback<ReturnItemRes> {
                override fun onResponse(
                    call: Call<ReturnItemRes>, response: Response<ReturnItemRes>
                ) {
                    Log.e("SalesListAPIResponse", "Response: ${response.body()}")
                    Log.e("SalesListAPIResponseX", "Response: ${Gson().toJson(response.body())}")
                    if (response.isSuccessful && response.body() != null) {
                        returnitem_data.postValue(response.body())
                        loading.postValue(ProgressData(isProgress = false))
                    } else {
                        loading.postValue(
                            ProgressData(
                                isProgress = false,
                                isMessage = true,
                                message = "Failed to fetch data, Try again"
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<ReturnItemRes>, t: Throwable) {
                    Log.d("rty", t.message.toString())
                    loading.postValue(
                        ProgressData(
                            isProgress = false, isMessage = true, message = "Something Went Wrong"
                        )
                    )
                }
            })
    }

    fun callReturnSalesSubmitApi(returnSaleReq: ReturnSaleReq, context: Context) {
        loading.postValue(ProgressData(isProgress = true))

        Log.e("SalesListAPIRequestreturn", "Request: ${returnSaleReq}")
        ApiClient().getApiService(context).getReturnSalesSubmitAPI(returnSaleReq)
            .enqueue(object : Callback<ReturnSaleRes> {
                override fun onResponse(
                    call: Call<ReturnSaleRes>, response: Response<ReturnSaleRes>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        returnsalesubmit_data.postValue(response.body())
                        loading.postValue(ProgressData(isProgress = false))
                    } else {
                        loading.postValue(
                            ProgressData(
                                isProgress = false,
                                isMessage = true,
                                message = "Failed to fetch data, Try again"
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<ReturnSaleRes>, t: Throwable) {
                    Log.d("rty", t.message.toString())
                    loading.postValue(
                        ProgressData(
                            isProgress = false, isMessage = true, message = "Something Went Wrong"
                        )
                    )
                }
            })
    }

    fun callReplaceSaleApi(req: ReplaceSaleReq, context: Context) {
        loading.postValue(ProgressData(isProgress = true))
        Log.e("ReplaceSaleRequest", "Request: $req")

        ApiClient().getApiService(context).replaceSale(req)
            .enqueue(object : Callback<ReturnSaleResRaw> {
                override fun onResponse(
                    call: Call<ReturnSaleResRaw>, response: Response<ReturnSaleResRaw>
                ) {
                    val code = response.code()
                    val ct = response.headers()["Content-Type"]
                    val peek = try { response.raw().peekBody(2048).string() } catch (_: Exception) { null }

                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body == null) {
                            Log.e("ReplaceSale", "Empty body (HTTP $code). Peek=${peek?.take(200)}")
                            loading.postValue(
                                ProgressData(false, true, "Empty server response")
                            )
                            return
                        }

                        try {
                            val normalized: ReturnSaleRes =
                                ReturnSaleResMapper.toReturnSaleRes(body, Gson())
                            returnsalesubmit_data.postValue(normalized)
                            loading.postValue(ProgressData(isProgress = false))
                        } catch (e: Exception) {
                            Log.e("ReplaceSale", "JSON parse failed", e)
                            Log.e("ReplaceSale", "Peek=${peek?.take(300)}")
                            loading.postValue(
                                ProgressData(false, true, "Response parsing failed")
                            )
                        }
                    } else {
                        val err = try { response.errorBody()?.string() } catch (_: Exception) { null }
                        Log.e("ReplaceSale", "HTTP $code ct=$ct err=${err ?: peek}")
                        loading.postValue(
                            ProgressData(false, true, "Failed to fetch data, Try again ($code)")
                        )
                    }
                }

                override fun onFailure(call: Call<ReturnSaleResRaw>, t: Throwable) {
                    Log.e("ReplaceSale", "Network/IO error", t)
                    loading.postValue(
                        ProgressData(false, true, "Something Went Wrong")
                    )
                }
            })
    }

    fun callSaleReturnReasonApi(context: Context) {
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).getReturnReasonAPI()
            .enqueue(object : Callback<SalesReturnReasonRes> {
                override fun onResponse(
                    call: Call<SalesReturnReasonRes>, response: Response<SalesReturnReasonRes>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        salesreturnreason_data.postValue(response.body())

                        // ✅ Save to local DB for offline use
                        viewModelScope.launch(Dispatchers.IO) {
                            saveReturnReasonsToLocalDB(response.body()!!.data)
                            Log.d("ReturnReasons", "💾 Saved ${response.body()!!.data.size} reasons to cache")
                        }

                        loading.postValue(ProgressData(isProgress = false))
                    } else {
                        loading.postValue(
                            ProgressData(
                                isProgress = false,
                                isMessage = true,
                                message = "Failed to fetch data, Try again"
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<SalesReturnReasonRes>, t: Throwable) {
                    Log.d("rty", t.message.toString())
                    loading.postValue(
                        ProgressData(
                            isProgress = false, isMessage = true, message = "Something Went Wrong"
                        )
                    )
                }
            })
    }

    // ✅ Batch fetch and cache all sales details (background, no blocking)
    fun batchCacheSalesDetails(context: Context, invoiceIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            invoiceIds.forEach { invoiceId ->
                try {
                    // Check if already cached
                    val exists = detailedSaleExistsInDB(invoiceId)

                    if (!exists) {
                        // Not cached, fetch from API (silently in background)
                        Log.d("BatchCache", "📡 Fetching: $invoiceId")

                        val response = ApiClient().getApiService(context)
                            .getReturnSalesItemAPI(ReturnItemReq(invoice_id = invoiceId))
                            .execute()  // Synchronous call in background thread

                        if (response.isSuccessful && response.body()?.data?.isNotEmpty() == true) {
                            val data = response.body()!!.data[0]
                            saveDetailedSaleToLocalDB(data)
                            Log.d("BatchCache", "💾 Cached: $invoiceId")
                        }
                    } else {
                        Log.d("BatchCache", "✅ Already cached: $invoiceId")
                    }
                } catch (e: Exception) {
                    Log.e("BatchCache", "❌ Failed: $invoiceId - ${e.message}")
                    // Continue with next invoice even if one fails
                }
            }
            Log.d("BatchCache", "🎉 Batch caching complete!")
        }
    }
}
