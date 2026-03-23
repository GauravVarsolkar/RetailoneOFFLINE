package com.retailone.pos.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.retailone.pos.localstorage.RoomDB.PendingSaleDao
import com.retailone.pos.localstorage.RoomDB.PendingSaleEntity
import com.retailone.pos.localstorage.RoomDB.PosDatabase
import com.retailone.pos.models.PointofsaleModel.PosSaleModel.PosSaleReq
import com.retailone.pos.models.PointofsaleModel.toPatchedJson
import com.retailone.pos.models.PosSalesDetailsModel.PosSalesDetails
import com.retailone.pos.localstorage.RoomDB.toStoreProData
import com.retailone.pos.network.ApiClient
import com.retailone.pos.localstorage.RoomDB.Converters
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


class PosSaleRepository(private val context: Context) {

    private val database = PosDatabase.getDatabase(context)
    private val dao: PendingSaleDao = database.pendingSaleDao()
    private val gson = Gson()
    private val apiService = ApiClient().getApiService(context)

    companion object {
        private const val TAG = "PosSaleRepository"
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
        // Check for duplicate invoice
        val existingSale = dao.getSaleByInvoice(saleReq.invoice_id, saleReq.store_id)
        if (existingSale != null) {
            onError("Invoice ${saleReq.invoice_id} already exists locally")
            return
        }

        if (NetworkUtils.isInternetAvailable(context)) {
            // ✅ ONLINE: Try API first
            submitToApiWithFallback(saleReq, onSuccess, onError)
        } else {
            // ✅ OFFLINE: Save locally
            saveSaleLocally(saleReq)
            onSuccess(createOfflineSuccessResponse(saleReq))
        }
    }

    /**
     * Try API, if it fails save locally
     */
    private suspend fun submitToApiWithFallback(
        saleReq: PosSaleReq,
        onSuccess: (PosSalesDetails) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val body = saleReq.toPatchedJson()

            apiService.posSale(body).enqueue(object : Callback<PosSalesDetails> {
                override fun onResponse(call: Call<PosSalesDetails>, response: Response<PosSalesDetails>) {
                    if (response.isSuccessful && response.body() != null) {
                        // ✅ API success
                        onSuccess(response.body()!!)
                    } else {
                        // ❌ API failed, save locally
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            saveSaleLocally(saleReq)
                        }
                        onSuccess(createOfflineSuccessResponse(saleReq))
                    }
                }

                override fun onFailure(call: Call<PosSalesDetails>, t: Throwable) {
                    // ❌ Network error, save locally
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        saveSaleLocally(saleReq)
                    }
                    onSuccess(createOfflineSuccessResponse(saleReq))
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "submitToApiWithFallback error: ${e.message}")
            saveSaleLocally(saleReq)
            onSuccess(createOfflineSuccessResponse(saleReq))
        }
    }

    /**
     * Save sale to Room database
     */
    private suspend fun saveSaleLocally(saleReq: PosSaleReq) {
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
                sync_status = "PENDING"
            )

            val saleId = dao.insertPendingSale(entity)
            Log.d("PosSaleRepository", "Sale saved locally with ID: $saleId, Invoice: ${saleReq.invoice_id}")

            // ✅ NEW: Deduct inventory from local database immediately
            deductInventoryAfterSale(saleReq)

        } catch (e: Exception) {
            Log.e("PosSaleRepository", "Error saving sale locally: ${e.message}")
        }
    }

    /**
     * ✅ UPDATED METHOD: Deduct inventory after offline sale
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
                    Log.d("STOCK_DEDUCTION", "BEFORE: stock_quantity=${productEntity.stock_quantity}")

                    val currentBatches = com.retailone.pos.localstorage.RoomDB.Converters().fromBatchJson(productEntity.batchJson)

                    Log.d("STOCK_DEDUCTION", "Current batches in DB:")
                    currentBatches.forEach { batch ->
                        Log.d("STOCK_DEDUCTION", "  Batch ${batch.batch_no}: qty=${batch.quantity}")
                    }

                    // Update each batch
                    val updatedBatches = currentBatches.map { dbBatch ->
                        val soldBatch = saleItem.batch.find { it.batchno == dbBatch.batch_no }

                        if (soldBatch != null) {
                            val qtySold = soldBatch.quantity.toDouble()
                            val newQty = (dbBatch.quantity - qtySold).coerceAtLeast(0.0)

                            Log.d("STOCK_DEDUCTION", "  Batch ${dbBatch.batch_no}: ${dbBatch.quantity} - $qtySold = $newQty")

                            com.retailone.pos.models.CommonModel.StroreProduct.PosSaleBatch(
                                batch_no = dbBatch.batch_no,
                                quantity = newQty,
                                price = dbBatch.price,
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

                    Log.d("STOCK_DEDUCTION", "AFTER: new stock_quantity=$newStockQty")

                    // Update StoreProductEntity
                    val updatedEntity = productEntity.copy(
                        stock_quantity = newStockQty,
                        batchJson = com.retailone.pos.localstorage.RoomDB.Converters().toBatchJson(updatedBatches),
                        lastUpdated = System.currentTimeMillis()
                    )

                    database.storeProductDao().insertProduct(updatedEntity)

                    // Verify the update
                    val verifyEntity = database.storeProductDao().getProductByKey(key)
                    Log.d("STOCK_DEDUCTION", "VERIFIED: stock in DB now = ${verifyEntity?.stock_quantity}")

                    // ✅ 2. Update ProductInventoryEntity (Product Inventory screen)
                    updateProductInventoryStock(saleReq.store_id, saleItem, newStockQty)

                } else {
                    Log.e("STOCK_DEDUCTION", "❌ Product not found in DB: key=$key")
                }
            }

            Log.d("STOCK_DEDUCTION", "========== STOCK DEDUCTION COMPLETE ==========")

        } catch (e: Exception) {
            Log.e("STOCK_DEDUCTION", "❌ Error: ${e.message}", e)
            e.printStackTrace()
        }
    }

    /**
     * ✅ NEW METHOD: Update Product Inventory table stock quantity
     */
    private suspend fun updateProductInventoryStock(
        storeId: String,
        saleItem: com.retailone.pos.models.PointofsaleModel.PosSaleModel.PosSalesItem,
        newStockQty: Double
    ) {
        try {
            val inventoryDao = database.productInventoryDao()

            // Calculate total quantity sold
            val totalQtySold = saleItem.batch.sumOf { it.quantity.toDouble() }

            // Find matching inventory items (there might be multiple with different composite keys)
            val allInventoryItems = inventoryDao.getInventoryByStoreSync(storeId.toInt())

            val matchingItems = allInventoryItems.filter {
                it.product_id == saleItem.product_id.toString() &&
                        it.distribution_pack_id == saleItem.distribution_pack_id.toString()
            }

            matchingItems.forEach { inventoryItem ->
                val updatedQty = (inventoryItem.stock_quantity - totalQtySold).coerceAtLeast(0.0)

                inventoryDao.updateStockQuantity(inventoryItem.compositeKey, updatedQty)

                Log.d("STOCK_DEDUCTION", "✅ Updated Product Inventory: ${inventoryItem.compositeKey}, new qty=$updatedQty")
            }

        } catch (e: Exception) {
            Log.e("STOCK_DEDUCTION", "❌ Error updating product inventory: ${e.message}", e)
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
                tax_amount = saleReq.tax_amount,
                discount = 0,
                customer_name = saleReq.customer_name,
                customer_mob_no = saleReq.customer_mob_no,
                vat_no = "",
                tpin_no = saleReq.tin_tpin_no,
                buyers_tpin = saleReq.tin_tpin_no,
                tax_ex = "",
                ej_no = "",
                ej_activation_date = "",
                tax_sdc_idamount = "",
                internal_data = "",
                receipt_sign = "",
                receipt_no = "",
                vsdc_reciept = null,
                taxrate = saleReq.tax,
                rcptType = ""
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
            // Mark as syncing
            dao.updateSyncStatus(sale.id, "SYNCING", System.currentTimeMillis())

            // Convert back to PosSaleReq
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
                invoice_id = sale.invoice_id
            )

            // Submit to API (synchronous for worker)
            val body = saleReq.toPatchedJson()
            val response = apiService.posSale(body).execute()

            return if (response.isSuccessful && response.body() != null) {
                // Success - mark as synced
                dao.markAsSynced(sale.id, System.currentTimeMillis())
                Log.d(TAG, "Sale ${sale.invoice_id} synced successfully")
                true
            } else {
                // Failed - mark as failed with error
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                dao.updateSyncStatusWithError(
                    sale.id,
                    "FAILED",
                    System.currentTimeMillis(),
                    errorMsg
                )
                Log.e(TAG, "Sale ${sale.invoice_id} sync failed: $errorMsg")
                false
            }

        } catch (e: Exception) {
            dao.updateSyncStatusWithError(
                sale.id,
                "FAILED",
                System.currentTimeMillis(),
                e.message ?: "Unknown error"
            )
            Log.e(TAG, "Sale ${sale.invoice_id} sync error: ${e.message}")
            return false
        }
    }

    /**
     * Sync all offline sales to server
     * Called by WorkManager in background
     */
    suspend fun syncOfflineSales(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Get all pending sales (not synced yet)
            val offlineSales = dao.getPendingSales()

            if (offlineSales.isEmpty()) {
                Log.d(TAG, "No offline sales to sync")
                return@withContext true
            }

            Log.d(TAG, "Found ${offlineSales.size} offline sales to sync")
            var successCount = 0
            var failCount = 0

            // Try to sync each sale
            for (pendingSale in offlineSales) {
                val syncResult = syncSale(pendingSale)
                if (syncResult) {
                    successCount++
                } else {
                    failCount++
                }
            }

            Log.d(TAG, "Sync completed: $successCount success, $failCount failed")
            return@withContext failCount == 0

        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Convert PosSalesItem to SalesItem format (used in return screen)
     */
    private fun convertToSalesItemFormat(saleReq: PosSaleReq): List<SalesItem> {
        // Get tax rate from main request (all items have same tax)
        val taxRateStr = saleReq.tax.replace("%", "").replace("@", "").trim()
        val taxRate = taxRateStr.toDoubleOrNull() ?: 0.0

        return saleReq.sales_items.map { posItem ->

            // Convert batches from PosSaleBatch to BatchReturnItem
            val batches = posItem.batch.map { posBatch ->
                // Calculate tax-exclusive price (reverse engineer from retail price)
                val taxMultiplier = 1.0 + (taxRate / 100.0)
                val taxExclusivePrice = if (taxRate > 0) {
                    posBatch.retail_price / taxMultiplier
                } else {
                    posBatch.retail_price
                }

                BatchReturnItem(
                    batch = posBatch.batchno,
                    quantity = posBatch.quantity.toDouble(),
                    retail_price = posBatch.retail_price,
                    tax_exclusive_price = taxExclusivePrice,
                    subtotal = posBatch.quantity * taxExclusivePrice,
                    sales_item_id = 0,
                    return_quantity = 0,
                    return_reason = null,
                    batch_return_quantity = 0,
                    batch_refund_amount = 0.0,
                    remark = null,
                    defective_boxes = 0,
                    defective_bottles = 0
                )
            }

            // Calculate total quantity from batches
            val totalQuantity = posItem.batch.sumOf { it.quantity.toDouble() }

            // Get retail price from first batch (all batches should have same price)
            val retailPrice = posItem.batch.firstOrNull()?.retail_price ?: 0.0

            // Calculate tax-exclusive price for the item
            val taxMultiplier = 1.0 + (taxRate / 100.0)
            val taxExclusivePrice = if (taxRate > 0) {
                retailPrice / taxMultiplier
            } else {
                retailPrice
            }

            // Get description from distribution_pack
            val distPackDescription = posItem.distribution_pack_name  // Already available!
            val noOfPacks = 1  // Default value since not available in PosSalesItem

            // NEW: include per-item taxes if available from API
            val itemTax = (posItem.tax ?: 0.0)
            val itemTaxAmount = (posItem.tax_amount ?: 0.0)
            val saleItemTax = itemTax.toInt()
            val saleItemTaxAmount = itemTaxAmount
            // ✅ Create SalesItem matching your exact structure
            SalesItem(
                created_at = saleReq.sale_date_time,
                distribution_pack = DistributionPack(
                    id = posItem.distribution_pack_id.toInt(),
                    no_of_packs = noOfPacks,
                    product_description = distPackDescription
                ),
                distribution_pack_id = posItem.distribution_pack_id.toInt(),
                distribution_pack_name = distPackDescription,
                id = 0,
                product = Product(
                    id = posItem.product_id.toInt(),
                    product_name = posItem.product_name  // ✅ Not nullable, no ?: needed
                ),
                product_id = posItem.product_id.toInt(),
                product_name = posItem.product_name,  // ✅ Not nullable
                quantity = totalQuantity,
                batches = batches,
                retail_price = retailPrice,
                tax_exclusive_price = taxExclusivePrice,
                sales_id = "0",
                status = 1,
                total_amount = posItem.total_amount.toDoubleOrNull() ?: 0.0,  // ✅ String to Double
                updated_at = saleReq.sale_date_time,
                tax = saleItemTax,
                tax_amount = saleItemTaxAmount,
                whole_sale_price = posItem.whole_sale_price.toDoubleOrNull() ?: 0.0,  // ✅ String to Double
                return_quantity = 0,
                refund_amount = 0.0,
                readonlyMode = false,
                isExpired = false
            )
        }
    }



}
