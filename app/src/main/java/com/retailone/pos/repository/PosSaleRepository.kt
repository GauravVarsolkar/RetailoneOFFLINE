package com.retailone.pos.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.retailone.pos.localstorage.RoomDB.PendingSaleDao
import com.retailone.pos.localstorage.RoomDB.PendingSaleEntity
import com.retailone.pos.localstorage.RoomDB.PosDatabase
import com.retailone.pos.models.PointofsaleModel.PosSaleModel.PosSaleReq
import com.retailone.pos.models.PointofsaleModel.toPatchedJson
import com.retailone.pos.models.PosSalesDetailsModel.PosSalesDetails
import com.retailone.pos.network.ApiClient
import com.retailone.pos.utils.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.retailone.pos.models.ReturnSalesItemModel.SalesItem
import com.retailone.pos.models.ReturnSalesItemModel.BatchReturnItem
import com.retailone.pos.models.ReturnSalesItemModel.Product
import com.retailone.pos.models.ReturnSalesItemModel.DistributionPack
import com.retailone.pos.localstorage.SharedPreference.SharedPrefHelper


class PosSaleRepository(private val context: Context) {

    private val database = PosDatabase.getDatabase(context)
    private val dao: PendingSaleDao = database.pendingSaleDao()
    private val gson = Gson()
    private val apiService = ApiClient().getApiService(context)
    private val sharedPrefHelper = SharedPrefHelper(context)

    companion object {
        private const val TAG = "PosSaleRepository"
    }

    private suspend fun generateNextOfflineInvoiceId(storeId: String): String {
        return try {
            val paymentDao = database.paymentInvoiceDao()
            val pendingDao = database.pendingSaleDao()
            
            val knownIds = mutableSetOf<String>()
            
            // 1. Get from SharedPreferences (Persistent across days/restarts)
            val spfId = sharedPrefHelper.getLastInvoiceId()
            Log.d("INVOICE_TRACKER", "Reading last saved invoice ID from SharedPreferences: ${spfId ?: "NONE"}")
            if (!spfId.isNullOrEmpty()) {
                knownIds.add(spfId)
            }
            

            // 3. Get from API cache (Current day's sales)
            val latestCache = paymentDao.getLatestPaymentInvoice(storeId.toIntOrNull() ?: 0)
            if (latestCache != null && latestCache.invoice_data_json.isNotEmpty()) {
                val invoiceRes = gson.fromJson(latestCache.invoice_data_json, com.retailone.pos.models.SalesPaymentModel.InvoicePayment.InvoiceRes::class.java)
                knownIds.addAll(invoiceRes.data.sales.map { it.invoice_id })
            }
            
            // 4. Get from current pending/failed sales in Room
            val pending = pendingDao.getPendingSales()
            knownIds.addAll(pending.map { it.invoice_id })
            
            var maxNum = 0L
            var prefix = "INV"
            var numFormatLength = 0
            
            val regex = Regex("^(.*?)(\\d+)$")
            
            for (id in knownIds) {
                if (id.startsWith("OFF_") || id.startsWith("OFF-")) continue
                
                val match = regex.find(id)
                if (match != null) {
                    val p = match.groupValues[1]
                    val numStr = match.groupValues[2]
                    val num = numStr.toLongOrNull() ?: 0L
                    
                    if (num >= maxNum) {
                        maxNum = num
                        prefix = p
                        numFormatLength = numStr.length
                    }
                }
            }
            
            if (maxNum > 0) {
                val nextNum = maxNum + 1
                val formatPattern = if (numFormatLength > 0) "%0${numFormatLength}d" else "%d"
                val nextId = "${prefix}${String.format(formatPattern, nextNum)}"
                
                Log.d(TAG, "Generated next offline ID: $nextId based on max observed: $maxNum (prefix: $prefix)")
                nextId
            } else {
                // Return a generic ID but don't save it to SharedPreferences
                val fallback = "OFF-${System.currentTimeMillis()}"
                Log.w(TAG, "Could not find base ID, falling back to $fallback")
                fallback
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating offline ID: ${e.message}")
            "OFF-${System.currentTimeMillis()}"
        }
    }

    /**
     * Submit sale (offline-first):
     * - If ONLINE: Try API first, save to Room only if API fails
     */
    suspend fun submitSale(
        saleReq: PosSaleReq,
        onSuccess: (PosSalesDetails) -> Unit,
        onError: (String) -> Unit
    ) {
        // Generate the offline fallback ID just in case
        val offlineInvoiceId = if (saleReq.invoice_id.isEmpty()) {
            generateNextOfflineInvoiceId(saleReq.store_id)
        } else {
            saleReq.invoice_id
        }

        if (NetworkUtils.isInternetAvailable(context)) {
            // ✅ ONLINE: Try API first with ORIGINAL request (so backend can auto-generate ID)
            submitToApiWithFallback(saleReq, offlineInvoiceId, onSuccess, onError)
        } else {
            // ✅ OFFLINE: Save locally with offline ID
            val finalSaleReq = saleReq.copy(invoice_id = offlineInvoiceId)
            
            // Check for duplicate invoice locally
            val existingSale = dao.getSaleByInvoice(finalSaleReq.invoice_id, finalSaleReq.store_id)
            if (existingSale != null) {
                onError("Invoice ${finalSaleReq.invoice_id} already exists locally")
                return
            }
            
            saveSaleLocally(finalSaleReq)
            onSuccess(createOfflineSuccessResponse(finalSaleReq))
        }
    }

    private suspend fun submitToApiWithFallback(
        saleReq: PosSaleReq,
        offlineInvoiceId: String,
        onSuccess: (PosSalesDetails) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val body = saleReq.toPatchedJson()

            apiService.posSale(body).enqueue(object : Callback<PosSalesDetails> {
                override fun onResponse(call: Call<PosSalesDetails>, response: Response<PosSalesDetails>) {
                    if (response.isSuccessful && response.body() != null) {
                        // ✅ API success
                        val serverPayload = response.body()!!
                        val serverInvoiceId = serverPayload.data?.invoice_id ?: offlineInvoiceId

                        // Save the last invoice ID to SharedPreferences for offline use
                        if (!serverInvoiceId.startsWith("OFF_") && !serverInvoiceId.startsWith("OFF-")) {
                            sharedPrefHelper.setLastInvoiceId(serverInvoiceId)
                        }

                        // Copy with the real server invoice ID to prevent duplicates if also returned by API list
                        val syncedSaleReq = saleReq.copy(invoice_id = serverInvoiceId)

                        // ✅ API success - ALSO save locally as SYNCED for offline records
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            saveSaleLocally(syncedSaleReq, syncStatus = "SYNCED")
                            // Check if we need to sync any already-recorded offline actions (unlikely in immediate flow but good for safety)
                            synchronizeOfflineActions(offlineInvoiceId, serverInvoiceId)
                        }
                        onSuccess(serverPayload)
                    } else {
                        // ❌ API failed, save locally with fallback ID
                        val offlineSaleReq = saleReq.copy(invoice_id = offlineInvoiceId)
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            saveSaleLocally(offlineSaleReq)
                        }
                        onSuccess(createOfflineSuccessResponse(offlineSaleReq))
                    }
                }

                override fun onFailure(call: Call<PosSalesDetails>, t: Throwable) {
                    // ❌ Network error, save locally with fallback ID
                    val offlineSaleReq = saleReq.copy(invoice_id = offlineInvoiceId)
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        saveSaleLocally(offlineSaleReq)
                    }
                    onSuccess(createOfflineSuccessResponse(offlineSaleReq))
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "submitToApiWithFallback error: ${e.message}")
            val offlineSaleReq = saleReq.copy(invoice_id = offlineInvoiceId)
            saveSaleLocally(offlineSaleReq)
            onSuccess(createOfflineSuccessResponse(offlineSaleReq))
        }
    }

    /**
     * Save sale to Room database
     */
    private suspend fun saveSaleLocally(saleReq: PosSaleReq, syncStatus: String = "PENDING") {
        try {
            val salesItemsJson = gson.toJson(saleReq.sales_items)

            // 🔍 Log full request JSON that will be stored
            val fullJson = GsonBuilder().setPrettyPrinting().create().toJson(saleReq)
            Log.d("OfflineSaleJSON", "Full PosSaleReq JSON to store:\n$fullJson")

            // 🔍 Log only items JSON
            Log.d("OfflineSaleJSON", "sales_items JSON stored in DB:\n$salesItemsJson")

            val entity = PendingSaleEntity(
                customer_mob_no = saleReq.customer_mob_no,
                customer_name = saleReq.customer_name,
                customer_id = saleReq.customer_id,
                discount_amount = saleReq.discount_amount,
                grand_total = saleReq.grand_total,
                payment_type = saleReq.payment_type,
                sales_items_json = salesItemsJson,
                sub_total = saleReq.sub_total,
                subtotal_after_discount = saleReq.subtotal_after_discount,
                tax = saleReq.tax,
                tax_amount = saleReq.tax_amount,
                store_id = saleReq.store_id,
                store_manager_id = saleReq.store_manager_id,
                amount_tendered = saleReq.amount_tendered,
                sale_date_time = saleReq.sale_date_time,
                tin_tpin_no = saleReq.tin_tpin_no,
                invoice_id = saleReq.invoice_id,
                trxn_code = saleReq.trxn_code,
                prc_no = saleReq.prc_no,
                spot_discount_percentage = saleReq.spot_discount_percentage,
                spot_discount_amount = saleReq.spot_discount_amount,
                sync_status = syncStatus
            )

            val saleId = dao.insertPendingSale(entity)
            Log.d("PosSaleRepository", "Sale saved locally with ID: $saleId, Invoice: ${saleReq.invoice_id}")

            // ✅ Save this invoice ID as the last known ID to SharedPreferences for better offline ID generation
            if (!saleReq.invoice_id.startsWith("OFF_") && !saleReq.invoice_id.startsWith("OFF-")) {
                Log.d("INVOICE_TRACKER", "Updating SharedPreferences with new local sale invoice ID: ${saleReq.invoice_id}")
                sharedPrefHelper.setLastInvoiceId(saleReq.invoice_id)
            }

            // ✅ Deduct inventory from local database immediately
            deductInventoryAfterSale(saleReq)

        } catch (e: Exception) {
            Log.e("PosSaleRepository", "Error saving sale locally: ${e.message}")
        }
    }

    /**
     * Deduct inventory after offline sale
     */
    private suspend fun deductInventoryAfterSale(saleReq: PosSaleReq) {
        try {
            Log.d("STOCK_DEDUCTION", "========== STARTING STOCK DEDUCTION ==========")

            saleReq.sales_items.forEach { saleItem ->
                val key = "${saleItem.product_id}_${saleItem.distribution_pack_id}"

                Log.d("STOCK_DEDUCTION", "Processing product: key=$key")

                // ✅ 1. Update StoreProductEntity (POS products)
                val productEntity = database.storeProductDao().getProductByKey(key)

                if (productEntity != null) {
                    val currentBatches = com.retailone.pos.localstorage.RoomDB.Converters().fromBatchJson(productEntity.batchJson)

                    // Update each batch
                    val updatedBatches = currentBatches.map { dbBatch ->
                        val soldBatch = saleItem.batch.find { it.batchno == dbBatch.batch_no }

                        if (soldBatch != null) {
                            val qtySold = soldBatch.quantity.toDouble()
                            val newQty = (dbBatch.quantity - qtySold).coerceAtLeast(0.0)

                            com.retailone.pos.models.CommonModel.StroreProduct.PosSaleBatch(
                                batch_no = dbBatch.batch_no,
                                quantity = newQty,
                                price = dbBatch.price,
                                tax = dbBatch.tax, 
                                batch_cart_quantity = 0.0,
                                batch_total_du_amount = dbBatch.batch_total_du_amount ?: "",
                                dispense_status = dbBatch.dispense_status,
                                discount = dbBatch.discount
                            )
                        } else {
                            dbBatch
                        }
                    }

                    // Calculate new total stock
                    val newStockQty = updatedBatches.sumOf { it.quantity }

                    // Update StoreProductEntity
                    val updatedEntity = productEntity.copy(
                        stock_quantity = newStockQty,
                        batchJson = com.retailone.pos.localstorage.RoomDB.Converters().toBatchJson(updatedBatches),
                        lastUpdated = System.currentTimeMillis()
                    )

                    database.storeProductDao().insertProduct(updatedEntity)

                    // ✅ 2. Update ProductInventoryEntity (Product Inventory screen)
                    updateProductInventoryStock(saleReq.store_id, saleItem, newStockQty)

                }
            }
            Log.d("STOCK_DEDUCTION", "========== STOCK DEDUCTION COMPLETE ==========")
        } catch (e: Exception) {
            Log.e("STOCK_DEDUCTION", "Error: ${e.message}")
        }
    }

    private suspend fun updateProductInventoryStock(storeId: String, saleItem: com.retailone.pos.models.PointofsaleModel.PosSaleModel.PosSalesItem, newStockQty: Double) {
        try {
            val inventoryDao = database.productInventoryDao()
            val totalQtySold = saleItem.batch.sumOf { it.quantity.toDouble() }
            val items = inventoryDao.getInventoryByStoreSync(storeId.toInt())
            items.filter { it.product_id == saleItem.product_id.toString() && it.distribution_pack_id == saleItem.distribution_pack_id.toString() }
                .forEach { item ->
                    val updated = (item.stock_quantity - totalQtySold).coerceAtLeast(0.0)
                    inventoryDao.updateStockQuantity(item.compositeKey, updated)
                }
        } catch (e: Exception) {
            Log.e("STOCK_DEDUCTION", "Error: ${e.message}")
        }
    }

    /**
     * Create a success response for offline sales
     */
    private fun createOfflineSuccessResponse(saleReq: PosSaleReq): PosSalesDetails {
        return PosSalesDetails(
            status = 1,
            message = "Sale saved offline. Will sync when online.",
            data = com.retailone.pos.models.PosSalesDetailsModel.Data(
                amount_tendered = saleReq.amount_tendered,
                discount_amount = saleReq.discount_amount,
                grand_total = saleReq.grand_total,
                invoice_id = saleReq.invoice_id,
                payment_type = saleReq.payment_type,
                purchase_date_time = saleReq.sale_date_time,
                salesItem = emptyList(),
                store = com.retailone.pos.models.PosSalesDetailsModel.Store(
                    address = "",
                    cluster_id = 0,
                    created_at = "",
                    deleted_at = "",
                    ho_manager_id = 0,
                    id = saleReq.store_id.toIntOrNull() ?: 0,
                    induction_date = "",
                    latitude = "",
                    location = "",
                    logo = "",
                    longitude = "",
                    organization_id = 0,
                    phone_no = "",
                    station_code = "",
                    status = 1,
                    store_name = "",
                    updated_at = ""
                ),
                store_manager_id = saleReq.store_manager_id,
                sub_total = saleReq.sub_total,
                subtotal_after_discount = saleReq.subtotal_after_discount,
                tax = saleReq.tax.toIntOrNull() ?: 0,
                discount = 0,
                tax_amount = saleReq.tax_amount,
                customer_name = saleReq.customer_name,
                customer_mob_no = saleReq.customer_mob_no,
                vat_no = "",
                tpin_no = saleReq.tin_tpin_no ?: "",
                buyers_tpin = saleReq.tin_tpin_no ?: "",
                tax_ex = "",
                ej_no = "",
                ej_activation_date = "",
                tax_sdc_idamount = "",
                internal_data = "",
                receipt_sign = "",
                receipt_no = "",
                vsdc_reciept = null,
                taxrate = saleReq.tax,
                rcptType = "",
                trxn_code = saleReq.trxn_code,
                tax_details = saleReq.tax_details,
                tax_summery = saleReq.tax_summery,
                spot_discount_percentage = saleReq.spot_discount_percentage?.toString(),
                spot_discount_amount = saleReq.spot_discount_amount
            )
        )
    }

    /**
     * Get all pending sales
     */
    suspend fun getPendingSales(): List<PendingSaleEntity> {
        return dao.getPendingSales()
    }

    /**
     * Get pending sales count as Flow (for UI badges)
     */
    fun getPendingSalesCountFlow(): Flow<Int> {
        return dao.getPendingSalesCountFlow()
    }

    /**
     * Sync a single pending sale
     */
    suspend fun syncSale(sale: PendingSaleEntity): Boolean {
        try {
            val currentSale = dao.getSaleById(sale.id)
            if (currentSale == null || currentSale.sync_status == "SYNCED" || currentSale.sync_status == "SYNCING") {
                return true
            }

            dao.updateSyncStatus(sale.id, "SYNCING", System.currentTimeMillis())

            val salesItems = gson.fromJson(
                sale.sales_items_json,
                Array<com.retailone.pos.models.PointofsaleModel.PosSaleModel.PosSalesItem>::class.java
            ).toList()

            val saleReq = PosSaleReq(
                customer_mob_no = sale.customer_mob_no,
                customer_name = sale.customer_name,
                customer_id = sale.customer_id,
                discount_amount = sale.discount_amount,
                grand_total = sale.grand_total,
                payment_type = sale.payment_type,
                sales_items = salesItems,
                sub_total = sale.sub_total,
                subtotal_after_discount = sale.subtotal_after_discount,
                tax = sale.tax,
                tax_amount = sale.tax_amount,
                store_id = sale.store_id,
                store_manager_id = sale.store_manager_id,
                amount_tendered = sale.amount_tendered,
                sale_date_time = sale.sale_date_time,
                tin_tpin_no = sale.tin_tpin_no,
                invoice_id = sale.invoice_id,
                trxn_code = sale.trxn_code ?: "",
                prc_no = sale.prc_no,
                tax_details = null,
                total_after_discount = 0,
                tax_summery = null,
                discount_rate = 0,
                spot_discount_percentage = sale.spot_discount_percentage ?: 0.0,
                spot_discount_amount = sale.spot_discount_amount ?: "0"
            )

            val body = saleReq.toPatchedJson()
            val response = apiService.posSale(body).execute()

            return if (response.isSuccessful && response.body() != null) {
                val serverInvoiceId = response.body()!!.data?.invoice_id ?: sale.invoice_id
                dao.markAsSyncedWithInvoiceId(sale.id, serverInvoiceId, System.currentTimeMillis())
                
                if (!serverInvoiceId.startsWith("OFF_") && !serverInvoiceId.startsWith("OFF-")) {
                    Log.d("INVOICE_TRACKER", "Updating SharedPreferences with server-confirmed invoice ID: $serverInvoiceId")
                    sharedPrefHelper.setLastInvoiceId(serverInvoiceId)
                }
                
                synchronizeOfflineActions(sale.invoice_id, serverInvoiceId)
                true
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                dao.updateSyncStatusWithError(sale.id, "FAILED", System.currentTimeMillis(), errorMsg)
                false
            }

        } catch (e: Exception) {
            dao.updateSyncStatusWithError(sale.id, "FAILED", System.currentTimeMillis(), e.message ?: "Unknown error")
            return false
        }
    }

    /**
     * Sync all offline sales to server
     */
    suspend fun syncOfflineSales(): Boolean = withContext(Dispatchers.IO) {
        try {
            val offlineSales = dao.getPendingSales()
            if (offlineSales.isEmpty()) return@withContext true
            var failCount = 0
            for (pendingSale in offlineSales) {
                if (!syncSale(pendingSale)) failCount++
            }
            return@withContext failCount == 0
        } catch (e: Exception) {
            return@withContext false
        }
    }

    /**
     * Synchronizes all dependent local actions (returns, replaces, details)
     */
    private suspend fun synchronizeOfflineActions(oldInvoiceId: String, newInvoiceId: String) {
        if (oldInvoiceId == newInvoiceId || oldInvoiceId.isEmpty()) return
        try {
            val detailedRepo = DetailedSaleRepository(context)
            detailedRepo.updateInvoiceId(oldInvoiceId, newInvoiceId)
            database.pendingReturnDao().updateInvoiceId(oldInvoiceId, newInvoiceId)
            database.pendingReplaceDao().updateInvoiceId(oldInvoiceId, newInvoiceId)
        } catch (e: Exception) {
            Log.e(TAG, "Optimization failed: ${e.message}")
        }
    }

}
