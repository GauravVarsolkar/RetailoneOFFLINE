package com.retailone.pos.viewmodels.DashboardViewodel

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.retailone.pos.localstorage.RoomDB.PendingSaleDao
import com.retailone.pos.localstorage.RoomDB.PendingSaleEntity
import com.retailone.pos.localstorage.RoomDB.PosDatabase
import com.retailone.pos.models.CashupModel.CashupDetails.CashupDetailsReq
import com.retailone.pos.models.CashupModel.CashupDetails.CashupDetailsRes
import com.retailone.pos.models.CashupModel.SendOTP.SendOtpRes
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.CancelSaleResponse
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.CancelSaleitemRequest
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.InvoiceReq
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.InvoiceRes
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.Sale
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.SalesDetailsReq
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.SalesDetailsRes
import com.retailone.pos.models.SalesPaymentModel.SalesList.SalesListReq
import com.retailone.pos.models.SalesPaymentModel.SalesList.SalesListRes
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.SalesDetailsData
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.SalesItem
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.Customer
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.StoreDetails
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.StoreManagerDetails
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.Summary
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.DistributionPack
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.Product
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.InvoiceData
import com.retailone.pos.network.ApiClient
import com.retailone.pos.repository.PaymentInvoiceRepository
import com.retailone.pos.repository.PendingCancelSaleRepository
import com.retailone.pos.repository.SalesDetailsRepository
import com.retailone.pos.utils.NetworkUtils
import com.retailone.pos.ui.Activity.DashboardActivity.SalesAndPaymentActivity
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SalesPaymentViewmodel:ViewModel() {

    private var paymentInvoiceRepository: PaymentInvoiceRepository? = null
    private var salesDetailsRepository: SalesDetailsRepository? = null
    private var pendingCancelSaleRepository: PendingCancelSaleRepository? = null

    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData : LiveData<ProgressData>
        get() = loading

    val saleslist_data = MutableLiveData<SalesListRes>()
    val saleslist_liveData: LiveData<SalesListRes>
        get() = saleslist_data

    val salesdetails_data = MutableLiveData<SalesDetailsRes>()
    val salesdetails_liveData: LiveData<SalesDetailsRes>
        get() = salesdetails_data


    val invoice_data = MutableLiveData<InvoiceRes>()
    val invoice_livedata: LiveData<InvoiceRes>
        get() = invoice_data

    /**
     * Initialize repositories with context (call this before using offline methods)
     */
    fun initRepository(context: Context) {
        if (paymentInvoiceRepository == null) {
            paymentInvoiceRepository = PaymentInvoiceRepository(context)
        }
        if (salesDetailsRepository == null) {
            salesDetailsRepository = SalesDetailsRepository(context)
        }
        if (pendingCancelSaleRepository == null) {
            pendingCancelSaleRepository = PendingCancelSaleRepository(context)
        }
    }


    fun callCancelSaleAPI(request: CancelSaleitemRequest, context: Context) {
        loading.postValue(ProgressData(isProgress = true))
        val gson = Gson()
        Log.d("SubmitRequestJSON", gson.toJson(request))

        ApiClient().getApiService(context).cancelItemAPI(request).enqueue(object : Callback<CancelSaleResponse> {
            override fun onResponse(call: Call<CancelSaleResponse>, response: Response<CancelSaleResponse>) {
                loading.postValue(ProgressData(isProgress = false))
                Log.d("SubmitResponse", response.body().toString())
                if (response.isSuccessful && response.body() != null) {
                    val res = response.body()!!
                    if (res.status == 1) {
                        Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()

                        // ✅ Redirect to another screen (update with your desired target Activity)
                        val intent = Intent(context, SalesAndPaymentActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Failed to cancel sale", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CancelSaleResponse>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false))
                Toast.makeText(context, "Something went wrong: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * ✅ NEW: Offline-aware cancel sale API
     * - If ONLINE: Calls API directly
     * - If OFFLINE: Queues the cancel request in local DB and syncs when online
     */
    fun callCancelSaleAPIOfflineAware(
        request: CancelSaleitemRequest,
        context: Context,
        saleId: Int,
        saleDateTime: String,
        storeId: String,
        grandTotal: String,
        paymentType: String
    ) {
        loading.postValue(ProgressData(isProgress = true))

        if (NetworkUtils.isInternetAvailable(context)) {
            // ✅ ONLINE: Call API directly
            Log.d("CancelSale", "🌐 Online - calling cancel API directly for invoice: ${request.invoiceID}")

            ApiClient().getApiService(context).cancelItemAPI(request).enqueue(object : Callback<CancelSaleResponse> {
                override fun onResponse(call: Call<CancelSaleResponse>, response: Response<CancelSaleResponse>) {
                    loading.postValue(ProgressData(isProgress = false))
                    Log.d("CancelSale", "Response: ${response.body()}")
                    if (response.isSuccessful && response.body() != null) {
                        val res = response.body()!!
                        if (res.status == 1) {
                            Toast.makeText(context, res.message ?: "Sale cancelled successfully", Toast.LENGTH_LONG).show()

                            // ✅ Redirect to SalesAndPaymentActivity
                            val intent = Intent(context, SalesAndPaymentActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, res.message ?: "Failed to cancel sale", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Failed to cancel sale", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CancelSaleResponse>, t: Throwable) {
                    loading.postValue(ProgressData(isProgress = false))
                    Toast.makeText(context, "Something went wrong: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            // ✅ OFFLINE: Queue the cancel request
            Log.d("CancelSale", "📴 Offline - queuing cancel request for invoice: ${request.invoiceID}")

            initRepository(context)

            // Check if a cancel request is already queued (avoid duplication)
            viewModelScope.launch {
                try {
                    val alreadyQueued = pendingCancelSaleRepository?.isCancelQueued(request.invoiceID) ?: false
                    if (alreadyQueued) {
                        loading.postValue(ProgressData(isProgress = false))
                        Toast.makeText(context, "Cancel request already queued for this invoice", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // Save the cancel request to local DB
                    val saved = pendingCancelSaleRepository?.saveCancelRequest(
                        invoiceId = request.invoiceID,
                        saleId = saleId,
                        saleDateTime = saleDateTime,
                        storeId = storeId,
                        grandTotal = grandTotal,
                        paymentType = paymentType
                    ) ?: false

                    if (saved) {
                        loading.postValue(ProgressData(isProgress = false))
                        Toast.makeText(context, "Cancel request queued. Will sync when online.", Toast.LENGTH_LONG).show()

                        // ✅ Redirect to SalesAndPaymentActivity
                        val intent = Intent(context, SalesAndPaymentActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        context.startActivity(intent)
                    } else {
                        loading.postValue(ProgressData(isProgress = false))
                        Toast.makeText(context, "Failed to queue cancel request", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    loading.postValue(ProgressData(isProgress = false))
                    Log.e("CancelSale", "❌ Error saving cancel request: ${e.message}", e)
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Check if a cancel request is queued for an invoice
     */
    suspend fun isCancelQueued(invoiceId: String, context: Context): Boolean {
        initRepository(context)
        return pendingCancelSaleRepository?.isCancelQueued(invoiceId) ?: false
    }

    /**
     * ✅ NEW: Get cancelled sales as negative Sale objects
     * These will be shown in SalesAndPaymentActivity with red/negative amounts
     */
    suspend fun getCancelledSalesAsNegativeSale(context: Context, startTime: Long? = null, endTime: Long? = null): List<Sale> {
        initRepository(context)
        return pendingCancelSaleRepository?.getCancelledSalesAsNegativeSale(startTime, endTime) ?: emptyList()
    }

    /**
     * ✅ NEW: Check if a sale is cancelled (has a pending cancel request)
     */
    suspend fun isSaleCancelled(saleId: Int, context: Context): Boolean {
        initRepository(context)
        return pendingCancelSaleRepository?.isSaleCancelled(saleId) ?: false
    }



    fun callSalesListApi(salesListReq: SalesListReq, context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).getSalesListAPI(salesListReq).enqueue(object :
            Callback<SalesListRes> {
            override fun onResponse(call: Call<SalesListRes>, response: Response<SalesListRes>) {

                if(response.isSuccessful && response.body()!=null){
                    saleslist_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<SalesListRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }

  /*  fun callSalesDetailsApi(salesDetailsReq: SalesDetailsReq, context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).getSalesDetailsAPI(salesDetailsReq).enqueue(object :
            Callback<SalesDetailsRes> {
            override fun onResponse(call: Call<SalesDetailsRes>, response: Response<SalesDetailsRes>) {

                if(response.isSuccessful && response.body()!=null){
                    salesdetails_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<SalesDetailsRes>, t: Throwable) {
                Log.d("xxx",t.message.toString())
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }*/

    fun callSalesDetailsApi(salesDetailsReq: SalesDetailsReq, context: Context) {
        val TAG = "SalesDetailsAPI"
        val gson = com.google.gson.Gson()
        val start = System.currentTimeMillis()

        // Log the request DTO before the call
        Log.d(TAG, "REQUEST BODY: ${gson.toJson(salesDetailsReq)}")

        loading.postValue(ProgressData(isProgress = true))

        val api = ApiClient().getApiService(context)
        val call = api.getSalesDetailsAPI(salesDetailsReq)

        // Log basic request info from OkHttp
        runCatching {
            val req = call.request()
            Log.d(TAG, "REQUEST URL: ${req.url}")
            Log.d(TAG, "REQUEST METHOD: ${req.method}")
            Log.d(TAG, "REQUEST HEADERS:\n${req.headers}")
        }

        call.enqueue(object : retrofit2.Callback<SalesDetailsRes> {
            override fun onResponse(
                call: retrofit2.Call<SalesDetailsRes>,
                response: retrofit2.Response<SalesDetailsRes>
            ) {
                val tookMs = System.currentTimeMillis() - start
                Log.d(TAG, "RESPONSE CODE: ${response.code()} (${response.message()}) in ${tookMs}ms")
                Log.d(TAG, "RESPONSE HEADERS:\n${response.headers()}")

                if (response.isSuccessful && response.body() != null) {
                    // Log successful JSON body
                    runCatching {
                        Log.d(TAG, "RESPONSE BODY: ${gson.toJson(response.body())}")
                    }.onFailure {
                        Log.w(TAG, "Failed to serialize response body for logging: ${it.message}")
                    }

                    salesdetails_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                } else {
                    // Log error body if present
                    val errorStr = runCatching { response.errorBody()?.string() ?: "" }
                        .getOrElse { "errorBody read failed: ${it.message}" }
                    Log.w(TAG, "RESPONSE ERROR BODY: $errorStr")

                    loading.postValue(
                        ProgressData(
                            isProgress = false,
                            isMessage = true,
                            message = "Failed to fetch data, Try again"
                        )
                    )
                }
            }

            override fun onFailure(call: retrofit2.Call<SalesDetailsRes>, t: Throwable) {
                val tookMs = System.currentTimeMillis() - start
                Log.e(TAG, "CALL FAILED in ${tookMs}ms: ${t.message}", t)
                loading.postValue(
                    ProgressData(
                        isProgress = false,
                        isMessage = true,
                        message = "Something Went Wrong"
                    )
                )
            }
        })
    }


    fun callInvoiceApi(invoiceReq: InvoiceReq, context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).getInvoiceAPI(invoiceReq).enqueue(object :
            Callback<InvoiceRes> {
            override fun onResponse(call: Call<InvoiceRes>, response: Response<InvoiceRes>) {

                if(response.isSuccessful && response.body()!=null){
                    invoice_data.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<InvoiceRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }

    // ✅ OFFLINE SUPPORT METHODS - Sales & Payment

    /**
     * Save invoice response to local cache for offline use
     */
    fun cacheInvoiceResponse(storeId: Int, fromDate: String, toDate: String, invoiceRes: InvoiceRes, context: Context) {
        initRepository(context)
        viewModelScope.launch {
            try {
                paymentInvoiceRepository?.savePaymentInvoice(storeId, fromDate, toDate, invoiceRes)
                Log.d("SalesPaymentViewModel", "✅ Cached invoice data for offline use")
            } catch (e: Exception) {
                Log.e("SalesPaymentViewModel", "❌ Error caching invoice: ${e.message}", e)
            }
        }
    }

    /**
     * Get cached invoice data for offline viewing (specific date range)
     */
    suspend fun getCachedInvoice(storeId: Int, fromDate: String, toDate: String): InvoiceRes? {
        return try {
            paymentInvoiceRepository?.getPaymentInvoiceByDateRange(storeId, fromDate, toDate)
        } catch (e: Exception) {
            Log.e("SalesPaymentViewModel", "❌ Error getting cached invoice: ${e.message}", e)
            null
        }
    }

    /**
     * Get latest cached invoice data (any date range) for offline use
     */
    suspend fun getLatestCachedInvoice(storeId: Int): InvoiceRes? {
        return try {
            paymentInvoiceRepository?.getLatestPaymentInvoice(storeId)
        } catch (e: Exception) {
            Log.e("SalesPaymentViewModel", "❌ Error getting latest cached invoice: ${e.message}", e)
            null
        }
    }

    /**
     * Check if invoice data exists in cache
     */
    suspend fun hasCachedInvoice(storeId: Int, fromDate: String, toDate: String): Boolean {
        return try {
            paymentInvoiceRepository?.paymentInvoiceExists(storeId, fromDate, toDate) ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Cleanup old cached invoices (older than 7 days)
     */
    fun cleanupOldCachedInvoices(context: Context) {
        initRepository(context)
        viewModelScope.launch {
            try {
                val deletedCount = paymentInvoiceRepository?.deleteOldPaymentInvoices() ?: 0
                Log.d("SalesPaymentViewModel", "🗑️ Cleaned up $deletedCount old cached invoices")
            } catch (e: Exception) {
                Log.e("SalesPaymentViewModel", "❌ Error cleaning up old invoices: ${e.message}", e)
            }
        }
    }

    // ✅ OFFLINE SUPPORT METHODS - Individual Sales Details

    /**
     * Save individual sales details to local cache for offline use
     */
    fun cacheSalesDetails(saleId: Int, invoiceId: String, salesDetailsRes: SalesDetailsRes, context: Context) {
        initRepository(context)
        viewModelScope.launch {
            try {
                salesDetailsRepository?.saveSalesDetails(saleId, invoiceId, salesDetailsRes)
                Log.d("SalesPaymentViewModel", "✅ Cached sales details for sale_id=$saleId")
            } catch (e: Exception) {
                Log.e("SalesPaymentViewModel", "❌ Error caching sales details: ${e.message}", e)
            }
        }
    }

    /**
     * Save multiple sales details to local cache (batch caching)
     */
    fun cacheMultipleSalesDetails(salesDetailsList: List<Triple<Int, String, SalesDetailsRes>>, context: Context) {
        initRepository(context)
        viewModelScope.launch {
            try {
                salesDetailsRepository?.saveMultipleSalesDetails(salesDetailsList)
                Log.d("SalesPaymentViewModel", "✅ Cached ${salesDetailsList.size} sales details in batch")
            } catch (e: Exception) {
                Log.e("SalesPaymentViewModel", "❌ Error caching multiple sales details: ${e.message}", e)
            }
        }
    }

    /**
     * Get cached sales details by sale ID for offline viewing
     */
    suspend fun getCachedSalesDetails(saleId: Int): SalesDetailsRes? {
        return try {
            salesDetailsRepository?.getSalesDetailsBySaleId(saleId)
        } catch (e: Exception) {
            Log.e("SalesPaymentViewModel", "❌ Error getting cached sales details: ${e.message}", e)
            null
        }
    }
 
    /**
     * Prefetch missing sales details from API and cache them locally.
     * This ensures all sales in the list are available offline without pre-clicking them.
     */
    fun prefetchMissingSalesDetails(sales: List<Sale>, context: Context) {
        if (!NetworkUtils.isInternetAvailable(context)) return
        
        initRepository(context)
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Limit to top 30 most recent sales to avoid spamming the API
            val recentSales = sales.take(30)
            var fetchedCount = 0
            
            for (sale in recentSales) {
                // Skip offline sales (they are already in pending_sales table)
                if (sale.id <= 0) continue
                
                // Skip if already cached
                if (salesDetailsRepository?.salesDetailsExists(sale.id) == true) continue
                
                try {
                    // Call API directly for prefetching
                    val response = ApiClient().getApiService(context).getSalesDetailsAPI(SalesDetailsReq(sale.id.toString())).execute()
                    if (response.isSuccessful && response.body() != null) {
                        val detailsRes = response.body()!!
                        if (detailsRes.status == 1) {
                            salesDetailsRepository?.saveSalesDetails(sale.id, sale.invoice_id, detailsRes)
                            fetchedCount++
                        }
                    }
                    // Small delay between calls to be nice to the server
                    kotlinx.coroutines.delay(100)
                } catch (e: Exception) {
                    Log.d("PREFETCH", "⚠️ Error prefetching sale ${sale.id}: ${e.message}")
                }
            }
            if (fetchedCount > 0) {
                Log.d("PREFETCH", "✅ Prefetched $fetchedCount new sales details for offline mode")
            }
        }
    }

    /**
     * Get cached sales details by invoice ID for offline viewing
     */
    suspend fun getCachedSalesDetailsByInvoiceId(invoiceId: String): SalesDetailsRes? {
        return try {
            salesDetailsRepository?.getSalesDetailsByInvoiceId(invoiceId)
        } catch (e: Exception) {
            Log.e("SalesPaymentViewModel", "❌ Error getting cached sales details by invoice: ${e.message}", e)
            null
        }
    }

    /**
     * Check if sales details exists in cache
     */
    suspend fun hasCachedSalesDetails(saleId: Int): Boolean {
        return try {
            salesDetailsRepository?.salesDetailsExists(saleId) ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if sales details exists by invoice ID
     */
    suspend fun hasCachedSalesDetailsByInvoiceId(invoiceId: String): Boolean {
        return try {
            salesDetailsRepository?.salesDetailsExistsByInvoiceId(invoiceId) ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Cleanup old cached sales details (older than 7 days)
     */
    fun cleanupOldCachedSalesDetails(context: Context) {
        initRepository(context)
        viewModelScope.launch {
            try {
                val deletedCount = salesDetailsRepository?.deleteOldSalesDetails() ?: 0
                Log.d("SalesPaymentViewModel", "🗑️ Cleaned up $deletedCount old cached sales details")
            } catch (e: Exception) {
                Log.e("SalesPaymentViewModel", "❌ Error cleaning up old sales details: ${e.message}", e)
            }
        }
    }

    // ✅ NEW: OFFLINE SALES METHODS

    /**
     * Get all offline sales from pending_sales table (for offline mode display)
     */
    suspend fun getOfflineSales(context: Context, storeId: Int, startTime: Long? = null, endTime: Long? = null): List<Sale> {
        return try {
            val database = PosDatabase.getDatabase(context)
            val pendingSaleDao = database.pendingSaleDao()
            
            val pendingSales = if (startTime != null && endTime != null) {
                pendingSaleDao.getSalesByDateRange(storeId.toString(), startTime, endTime)
            } else {
                pendingSaleDao.getPendingSales().filter { it.store_id == storeId.toString() }
            }
            
            pendingSales
                .map { convertPendingSaleToSale(context, it) }
                .sortedByDescending { it.created_at }
        } catch (e: Exception) {
            Log.e("SalesPaymentViewModel", "❌ Error getting offline sales: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get all offline sales from pending_sales table (including synced ones, for offline mode)
     */
    suspend fun getAllOfflineSales(context: Context, storeId: Int): List<Sale> {
        return try {
            val database = PosDatabase.getDatabase(context)
            val pendingSaleDao = database.pendingSaleDao()
            val pendingSales = pendingSaleDao.getPendingSales()
            
            pendingSales
                .filter { it.store_id == storeId.toString() }
                .map { convertPendingSaleToSale(context, it) }
                .sortedByDescending { it.created_at }
        } catch (e: Exception) {
            Log.e("SalesPaymentViewModel", "❌ Error getting all offline sales: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Convert PendingSaleEntity to Sale model for display in SalesAndPayment screen
     */
    private fun convertPendingSaleToSale(context: Context, pendingSale: PendingSaleEntity): Sale {
        return Sale(
            id = pendingSale.id,
            invoice_id = pendingSale.invoice_id,
            grand_total = pendingSale.grand_total.toDoubleOrNull() ?: 0.0,
            sub_total = pendingSale.sub_total.toDoubleOrNull() ?: 0.0,
            subtotal_after_discount = pendingSale.subtotal_after_discount.toDoubleOrNull() ?: 0.0,
            discount_amount = pendingSale.discount_amount.toDoubleOrNull() ?: 0.0,
            // ✅ NEW: Parse tax from string (handle formats like "18", "@18%", "18%")
            tax = parseTaxPercentage(pendingSale.tax),
            tax_amount = pendingSale.tax_amount.toDoubleOrNull() ?: 0.0,
            payment_type = pendingSale.payment_type,
            store_id = pendingSale.store_id.toIntOrNull() ?: 0,
            store_manager_id = pendingSale.store_manager_id.toIntOrNull() ?: 0,
            amount_tendered = pendingSale.amount_tendered.toDoubleOrNull() ?: 0.0,
            // ✅ NEW: Format date for API display (convert from "dd-MMM-yyyy hh:mm a" to ISO format)
            created_at = formatDateForApi(context, pendingSale.sale_date_time),
            updated_at = formatDateForApi(context, pendingSale.sale_date_time),
            status = 1 // Active status
        )
    }

    /**
     * ✅ NEW: Parse tax percentage from string (handles formats like "18", "@18%", "18%")
     */
    private fun parseTaxPercentage(taxString: String): Double {
        if (taxString.isEmpty()) {
            Log.d("TAX_DEBUG", "⚠️ parseTaxPercentage: Empty string, returning 0.0")
            return 0.0
        }
        return try {
            val cleaned = taxString.trim()
                .replace(Regex("[^0-9.,]"), "")
                .replace(',', '.')
            Log.d("TAX_DEBUG", "📊 parseTaxPercentage: input='$taxString', cleaned='$cleaned'")
            val result = cleaned.toDoubleOrNull() ?: 0.0
            Log.d("TAX_DEBUG", "📊 parseTaxPercentage: result=$result")
            result
        } catch (e: Exception) {
            Log.e("TAX_DEBUG", "❌ parseTaxPercentage error: ${e.message}")
            0.0
        }
    }

    /**
     * ✅ NEW: Parse tax amount from string (handles formats like "180.00", "ZWL 180.00", "1,800.00", etc.)
     */
    private fun parseTaxAmount(amountString: String): Double {
        if (amountString.isEmpty()) return 0.0
        return try {
            val cleaned = amountString.trim()
                .replace(Regex("[^0-9.,]"), "")
                .replace(',', '.')
            cleaned.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            Log.e("SalesPaymentViewModel", "❌ Error parsing tax amount: '$amountString' - ${e.message}")
            0.0
        }
    }

    /**
     * ✅ NEW: Format date from "dd-MMM-yyyy hh:mm a" to "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" for DateTimeFormatting
     * Example: "20-Mar-2026 02:30 PM" -> "2026-03-20T14:30:00.000Z"
     */
    private fun formatDateForApi(context: Context, inputDate: String): String {
        if (inputDate.isEmpty()) return inputDate
        
        // ✅ If already ISO format, don't re-parse
        if (inputDate.contains("T") && inputDate.endsWith("Z")) return inputDate

        return try {
            val localizationData = com.retailone.pos.localstorage.SharedPreference.LocalizationHelper(context).getLocalizationData()
            val zone = localizationData.timezone
            
            // Map store zone to full TimeZone ID
            val timezoneId = when (zone) {
                "IST" -> "Asia/Kolkata"
                "CAT" -> "Africa/Lusaka"
                else -> "Africa/Lusaka"
            }

            val inputFormat = java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm a", java.util.Locale.ENGLISH)
            inputFormat.timeZone = java.util.TimeZone.getTimeZone(timezoneId) // ✅ Parse as Store Timezone
            
            val outputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.ENGLISH)
            outputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC") // ✅ Output as UTC
            
            val date = inputFormat.parse(inputDate)
            outputFormat.format(date ?: throw Exception("Null date"))
        } catch (e: Exception) {
            Log.e("SalesPaymentViewModel", "❌ Error formatting date: $inputDate - ${e.message}")
            inputDate // Return original if parsing fails
        }
    }

    /**
     * Get offline sale details by sale ID (for viewing offline sale details)
     */
    suspend fun getOfflineSaleDetails(context: Context, saleId: Int): SalesDetailsRes? {
        return try {
            val database = PosDatabase.getDatabase(context)
            val pendingSaleDao = database.pendingSaleDao()
            
            // ✅ RESOLVE ID: 
            // Range -1 to -1000000 is for PendingSaleEntity
            // Range -1000001 to -2000000 is for PendingCancelSaleEntity (reversals)
            
            if (saleId in -2000000..-1000001) {
                val cancelId = kotlin.math.abs(saleId) - 1000000
                Log.d("OFFLINE_DETAILS_DEBUG", "🔍 Resolving negative reversal ID ($saleId) to cancel request ID ($cancelId)")
                val cancelRequest = database.pendingCancelSaleDao().getAllCancels().find { it.id == cancelId }
                if (cancelRequest != null) {
                    val originalSale = pendingSaleDao.getSaleById(cancelRequest.sale_id)
                    if (originalSale != null) {
                        val originalDetails = convertPendingSaleToSalesDetailsData(context, originalSale)
                        // Create a "Reversal" version of details
                        return originalDetails.copy(
                            grand_total = -originalDetails.grand_total,
                            sub_total = -originalDetails.sub_total,
                            tax_amount = -originalDetails.tax_amount,
                            invoice_id =  originalDetails.invoice_id
                        ).let {
                            SalesDetailsRes(status = 1, message = "Cancelled Offline Sale", data = listOf(it))
                        }
                    }
                }
            }

            val realId = if (saleId in -1000000..-1) {
                val absId = kotlin.math.abs(saleId)
                Log.d("OFFLINE_DETAILS_DEBUG", "🔍 Resolving negative saleId ($saleId) to real database ID ($absId)")
                absId
            } else {
                Log.d("OFFLINE_DETAILS_DEBUG", "🔍 Using positive saleId ($saleId) directly")
                saleId
            }
            
            val pendingSale = pendingSaleDao.getSaleById(realId)
            
            if (pendingSale != null) {
                Log.d("OFFLINE_DETAILS_DEBUG", "✅ Record found in pending_sales table for ID: $realId")
                Log.d("OFFLINE_DETAILS_DEBUG", "   - Invoice: ${pendingSale.invoice_id}")
                Log.d("OFFLINE_DETAILS_DEBUG", "   - Grand Total: ${pendingSale.grand_total}")
                // ✅ NEW: Debug log tax values from database
                Log.d("TAX_DEBUG", "📦 getOfflineSaleDetails: Retrieved from DB")
                Log.d("TAX_DEBUG", "   pendingSale.tax = '${pendingSale.tax}'")
                Log.d("TAX_DEBUG", "   pendingSale.tax_amount = '${pendingSale.tax_amount}'")
                
                val salesDetailsData = convertPendingSaleToSalesDetailsData(context, pendingSale)
                Log.d("TAX_DEBUG", "📦 After conversion: salesDetailsData.tax = ${salesDetailsData.tax}")
                
                SalesDetailsRes(
                    status = 1,
                    message = "Offline sale",
                    data = listOf(salesDetailsData)
                )
            } else {
                Log.d("TAX_DEBUG", "❌ getOfflineSaleDetails: Sale not found for id=$saleId")
                null
            }
        } catch (e: Exception) {
            Log.e("SalesPaymentViewModel", "❌ Error getting offline sale details: ${e.message}", e)
            null
        }
    }

    /**
     * Get offline sale details by invoice ID
     */
    suspend fun getOfflineSaleDetailsByInvoice(context: Context, invoiceId: String, storeId: Int): SalesDetailsRes? {
        return try {
            val database = PosDatabase.getDatabase(context)
            val pendingSaleDao = database.pendingSaleDao()
            val pendingSale = pendingSaleDao.getSaleByInvoice(invoiceId, storeId.toString())
            
            if (pendingSale != null) {
                val salesDetailsData = convertPendingSaleToSalesDetailsData(context, pendingSale)
                SalesDetailsRes(
                    status = 1,
                    message = "Offline sale",
                    data = listOf(salesDetailsData)
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("SalesPaymentViewModel", "❌ Error getting offline sale details by invoice: ${e.message}", e)
            null
        }
    }

    /**
     * Convert PendingSaleEntity to SalesDetailsData for viewing sale details offline
     */
    private fun convertPendingSaleToSalesDetailsData(context: Context, pendingSale: PendingSaleEntity): SalesDetailsData {
        val gson = Gson()
        
        // ✅ NEW: Get sale-level tax values for distribution to items
        val saleTaxPercent = parseTaxPercentage(pendingSale.tax)
        val saleTaxAmount = parseTaxAmount(pendingSale.tax_amount)
        val saleSubTotal = pendingSale.sub_total.toDoubleOrNull() ?: 0.0
        
        Log.d("TAX_DEBUG", "📦 Sale-level tax: percent=$saleTaxPercent%, amount=$saleTaxAmount, subtotal=$saleSubTotal")
        
        // Parse sales items JSON if available
        val salesItems: List<SalesItem> = try {
            val items = gson.fromJson(pendingSale.sales_items_json, Array<com.retailone.pos.models.PointofsaleModel.PosSaleModel.PosSalesItem>::class.java)
            items?.mapIndexed { index, posItem ->
                val itemTotal = posItem.total_amount.toDoubleOrNull() ?: 0.0
                val itemSubTotal = posItem.whole_sale_price.toDoubleOrNull() ?: 0.0
                val itemQuantity = posItem.batch.sumOf { it.quantity.toDouble() }
                // NEW: use per-item taxes from offline payload when available
                val itemTax = (posItem.tax ?: 0.0)
                val itemTaxAmount = (posItem.tax_amount ?: 0.0)
                // ✅ Create SalesItem matching your exact structure
                SalesItem(
                    id = index + 1,
                    product_id = posItem.product_id.toIntOrNull() ?: 0,
                    product_name = posItem.product_name.ifEmpty { "Product" },
                    quantity = itemQuantity,
                    retail_price = posItem.whole_sale_price.toDoubleOrNull() ?: 0.0,
                    total_amount = itemTotal,
                    sub_total = itemSubTotal,
                    // NEW: per-item taxes (no distribution)
                    tax = itemTax.toInt(),
                    whole_sale_price = posItem.whole_sale_price.toDoubleOrNull() ?: 0.0,
                    tax_amount = itemTaxAmount,
                    discount = posItem.discount ?: 0,
                    distribution_pack_id = posItem.distribution_pack_id.toIntOrNull() ?: 0,
                    distribution_pack_name = posItem.distribution_pack_name,
                    total_quantity = itemQuantity,
                    created_at = "",
                    updated_at = "",
                    status = 1,
                    sales_id = pendingSale.invoice_id,
                    distribution_pack = DistributionPack(id = posItem.distribution_pack_id.toIntOrNull() ?: 0, no_of_packs = 1, product_description = ""),
                    product = Product(id = posItem.product_id.toIntOrNull() ?: 0, product_name = posItem.product_name.ifEmpty { "Product" })
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("SalesPaymentViewModel", "❌ Error parsing sales items JSON: ${e.message}")
            emptyList()
        }

        return SalesDetailsData(
            id = pendingSale.id,
            invoice_id = pendingSale.invoice_id,
            grand_total = pendingSale.grand_total.toDoubleOrNull() ?: 0.0,
            sub_total = pendingSale.sub_total.toDoubleOrNull() ?: 0.0,
            subtotal_after_discount = pendingSale.subtotal_after_discount.toDoubleOrNull() ?: 0.0,
            discount_amount = pendingSale.discount_amount.toDoubleOrNull() ?: 0.0,
            discount = pendingSale.discount_amount.toIntOrNull() ?: 0,
            // ✅ NEW: Parse tax from string (handle formats like "18", "@18%", "18%")
            tax = saleTaxPercent.toInt(),
            // ✅ NEW: Parse tax_amount from string (handle formats like "180.00", "ZWL 180.00", etc.)
            tax_amount = saleTaxAmount,
            payment_type = pendingSale.payment_type,
            store_id = pendingSale.store_id.toIntOrNull() ?: 0,
            store_manager_id = pendingSale.store_manager_id.toIntOrNull() ?: 0,
            amount_tendered = pendingSale.amount_tendered.toDoubleOrNull()?.toInt() ?: 0,
            // ✅ NEW: Format date for display (convert from "dd-MMM-yyyy hh:mm a" to "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            created_at = formatDateForApi(context, pendingSale.sale_date_time),
            updated_at = formatDateForApi(context, pendingSale.sale_date_time),
            status = 1,
            sales_items = salesItems,
            customer = Customer(
                id = pendingSale.customer_id,
                customer_name = pendingSale.customer_name.ifEmpty { "N/A" }.takeIf { it.isNotEmpty() },
                customer_mob_no = pendingSale.customer_mob_no.ifEmpty { "N/A" },
                created_at = "",
                updated_at = "",
                sales_id = pendingSale.invoice_id,
                status = 1
            ),
            store_details = StoreDetails(
                id = pendingSale.store_id.toIntOrNull() ?: 0,
                store_name = com.retailone.pos.localstorage.SharedPreference.ProfileAttendanceHelper(context).getUserProfile()?.data?.user_details?.store_name ?: "N/A",
                address = "N/A",
                cluster_id = 0,
                created_at = "",
                deleted_at = "",
                ho_manager_id = 0,
                induction_date = "",
                latitude = "",
                location = "",
                logo = "",
                longitude = "",
                organization_id = 0,
                phone_no = "",
                station_code = "",
                status = 1,
                updated_at = ""
            ),
            store_manager_details = StoreManagerDetails(
                id = pendingSale.store_manager_id.toIntOrNull() ?: 0,
                active_status = "",
                address = "",
                allow_login = 0,
                alt_contact_no = "",
                business_id = "",
                cluster_id = "",
                contact_no = "",
                created_at = "",
                current_address = "",
                deleted_at = "",
                dob = "",
                email = "",
                email_verified_at = "",
                first_name = "",
                gender = "",
                last_name = "",
                password = "",
                permanent_address = "",
                role_id = "",
                status = 0,
                surname = "",
                updated_at = "",
                user_type = "",
                username = ""
            ),
            total_refunded_amount = 0.0,
            summary = Summary(
                total_sub_total = pendingSale.sub_total.toDoubleOrNull() ?: 0.0,
                // ✅ NEW: Parse tax_amount from string for summary
                total_tax_amount = saleTaxAmount,
                total_total_amount = pendingSale.grand_total.toDoubleOrNull() ?: 0.0
            ),
            spot_discount_percentage = pendingSale.spot_discount_percentage?.toString(),
            spot_discount_amount = pendingSale.spot_discount_amount?.toString()
        )
    }

    /**
     * Check if a sale ID is an offline sale
     */
    suspend fun isOfflineSale(context: Context, saleId: Int): Boolean {
        return try {
            val database = PosDatabase.getDatabase(context)
            val pendingSaleDao = database.pendingSaleDao()
            // ✅ RESOLVE ID: Only check pending_sales if ID is in the offline sale range (-1 to -1000000)
            val realId = if (saleId in -1000000..-1) kotlin.math.abs(saleId) else saleId
            pendingSaleDao.getSaleById(realId) != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if a sale ID is an offline sale by invoice ID
     */
    suspend fun isOfflineSaleByInvoice(context: Context, invoiceId: String, storeId: Int): Boolean {
        return try {
            val database = PosDatabase.getDatabase(context)
            val pendingSaleDao = database.pendingSaleDao()
            pendingSaleDao.getSaleByInvoice(invoiceId, storeId.toString()) != null
        } catch (e: Exception) {
            false
        }
    }


}
