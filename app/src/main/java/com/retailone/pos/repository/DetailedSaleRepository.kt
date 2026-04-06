package com.retailone.pos.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.retailone.pos.localstorage.RoomDB.DetailedSaleDao
import com.retailone.pos.localstorage.RoomDB.DetailedSaleEntity
import com.retailone.pos.localstorage.RoomDB.PosDatabase
import com.retailone.pos.models.ReturnSalesItemModel.BatchReturnItem
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemData
import com.retailone.pos.models.ReturnSalesItemModel.SalesReturn
import com.retailone.pos.models.ReturnSalesItemModel.SalesReturnReason

class DetailedSaleRepository(private val context: Context) {

    private val database = PosDatabase.getDatabase(context)
    private val dao: DetailedSaleDao = database.detailedSaleDao()
    private val gson = Gson()

    companion object {
        private const val TAG = "DetailedSaleRepo"
        private const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1000
    }

    /**
     * Save detailed sale data to local database
     */
    suspend fun saveDetailedSale(returnItemData: ReturnItemData) {
        try {
            val entity = DetailedSaleEntity(
                invoice_id = returnItemData.invoice_id ?: "",
                sale_id = returnItemData.id,
                detailed_data_json = gson.toJson(returnItemData)
            )

            dao.insertDetailedSale(entity)
            Log.d(TAG, "✅ Saved detailed sale: ${returnItemData.invoice_id}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving detailed sale: ${e.message}", e)
        }
    }

    /**
     * Get detailed sale by invoice ID (for offline viewing)
     */
    suspend fun getDetailedSaleByInvoiceId(invoiceId: String): ReturnItemData? {
        return try {
            val entity = dao.getDetailedSaleByInvoiceId(invoiceId)
            if (entity != null) {
                gson.fromJson(entity.detailed_data_json, ReturnItemData::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error retrieving detailed sale: ${e.message}", e)
            null
        }
    }

    /**
     * Get the raw database entity for a detailed sale
     */
    suspend fun getDetailedSaleEntityByInvoiceId(invoiceId: String): DetailedSaleEntity? {
        return dao.getDetailedSaleByInvoiceId(invoiceId)
    }

    /**
     * Check if detailed sale exists in database
     */
    suspend fun detailedSaleExists(invoiceId: String): Boolean {
        return dao.detailedSaleExists(invoiceId)
    }

    /**
     * Delete detailed sales older than 7 days
     */
    suspend fun deleteOldDetailedSales(): Int {
        val sevenDaysAgo = System.currentTimeMillis() - SEVEN_DAYS_MILLIS
        val deletedCount = dao.deleteDetailedSalesOlderThan(sevenDaysAgo)
        Log.d(TAG, "🗑️ Deleted $deletedCount old detailed sales")
        return deletedCount
    }

    /**
     * Update total_refunded_amount and items (Support partial returns offline)
     */
    suspend fun updateRefundedAmount(
        invoiceId: String,
        refundedAmount: Double,
        reasonId: Int,
        returnedItems: List<BatchReturnItem>? = null
    ) {
        try {
            Log.d(TAG, "🔍 updateRefundedAmount: invoice=$invoiceId, amount=$refundedAmount, partial=${returnedItems != null}")

            val entity = dao.getDetailedSaleByInvoiceId(invoiceId)
            if (entity != null) {
                val saleData = gson.fromJson(entity.detailed_data_json, ReturnItemData::class.java)

                // ✅ Update individual items and batches
                val reasonName = getReasonNameById(reasonId) ?: "Offline Return"
                val updatedItems = saleData.salesItems?.map { si ->
                    val providedReturnedBatches = returnedItems?.filter { it.sales_item_id == si.id }
                    
                    Log.d("RETAILONE_DEBUG", "Processing item ${si.product_name} (ID: ${si.id}). Found ${providedReturnedBatches?.size ?: 0} returned batches.")

                    if (returnedItems != null) {
                        // PARTIAL RETURN: Update ONLY specified batches
                        val updatedBatches = si.batches?.map { batch ->
                            val match = providedReturnedBatches?.find { 
                                it.batch?.trim()?.equals(batch.batch?.trim() ?: "", ignoreCase = true) == true
                            }
                            if (match != null) {
                                Log.d("RETAILONE_DEBUG", "   - Match found for batch ${batch.batch}: qty=${match.batch_return_quantity}")
                                batch.copy(
                                    return_quantity = match.batch_return_quantity ?: match.return_quantity ?: 0,
                                    batch_return_quantity = match.batch_return_quantity ?: match.return_quantity ?: 0
                                )
                            } else {
                                batch
                            }
                        }
                        si.copy(
                            return_quantity = updatedBatches?.sumOf { it.batch_return_quantity ?: 0 } ?: 0,
                            batches = updatedBatches,
                            return_reason = reasonName // ✅ Track reason at item level too
                        )
                    } else {
                        // FULL RETURN: Mark everything as returned
                        Log.d("RETAILONE_DEBUG", "   - Marking FULL RETURN for item ${si.product_name}")
                        val updatedBatches = si.batches?.map { batch ->
                            batch.copy(
                                return_quantity = batch.quantity?.toInt() ?: 0,
                                batch_return_quantity = batch.quantity?.toInt() ?: 0
                            )
                        }
                        si.copy(
                            return_quantity = si.quantity.toInt(),
                            batches = updatedBatches,
                            return_reason = reasonName // ✅ Track reason at item level too
                        )
                    }
                } ?: emptyList()

                // Also update the snake_case list
                val updatedDetailedItems = saleData.sales_items?.map { si ->
                    val providedReturnedBatches = returnedItems?.filter { it.sales_item_id == si.id }
                    val returnQty = if (returnedItems != null) {
                        providedReturnedBatches?.sumOf { it.batch_return_quantity ?: it.return_quantity ?: 0 }?.toDouble() ?: 0.0
                    } else {
                        si.quantity
                    }

                    if (returnQty > 0) {
                        si.copy(sales_returns = listOf(
                            SalesReturn(
                                id = 0,
                                sales_id = si.id ?: 0,
                                invoice_id = invoiceId ?: "",
                                sales_item_id = si.id ?: 0,
                                return_quantity = returnQty,
                                reason_id = reasonId,
                                rate = si.retail_price,
                                amount = returnQty * si.retail_price,
                                reason = SalesReturnReason(reasonId, reasonName)
                            )
                        ))
                    } else {
                        si.copy(sales_returns = emptyList())
                    }
                } ?: emptyList()

                val updatedSaleData = saleData.copy(
                    salesItems = updatedItems,
                    sales_items = updatedDetailedItems,
                    total_refunded_amount = refundedAmount,
                    reason_id = reasonId
                )

                val updatedEntity = entity.copy(
                    detailed_data_json = gson.toJson(updatedSaleData),
                    created_at = System.currentTimeMillis()
                )

                dao.insertDetailedSale(updatedEntity)
                Log.d("RETAILONE_DEBUG", "✅ updateRefundedAmount SUCCESS: invoice=$invoiceId, total_refunded=$refundedAmount")
            }
        } catch (e: Exception) {
            Log.e("RETAILONE_DEBUG", "❌ updateRefundedAmount ERROR: ${e.message}")
        }
    }

    /**
     * Update total_replaced_amount for a sale (after successful replace)
     */
    suspend fun updateReplacedAmount(invoiceId: String, replacedAmount: Double, reasonId: Int) {
        try {
            val entity = dao.getDetailedSaleByInvoiceId(invoiceId)
            if (entity != null) {
                val saleData = gson.fromJson(entity.detailed_data_json, ReturnItemData::class.java)
                
                // ✅ Update individual items to reflect replace quantity
                val updatedItems = saleData.salesItems?.map { si ->
                    val updatedBatches = si.batches?.map { batch ->
                        batch.copy(
                            return_quantity = batch.quantity?.toInt() ?: 0,
                            batch_return_quantity = batch.quantity?.toInt() ?: 0
                        )
                    }
                    si.copy(
                        return_quantity = si.quantity.toInt(),
                        batches = updatedBatches
                    )
                } ?: emptyList()

                val updatedSaleData = saleData.copy(
                    total_replaced_amount = replacedAmount,
                    reason_id = reasonId,
                    salesItems = updatedItems
                )
                val updatedEntity = entity.copy(
                    detailed_data_json = gson.toJson(updatedSaleData),
                    created_at = System.currentTimeMillis()
                )
                dao.insertDetailedSale(updatedEntity)
                Log.d("RETAILONE_DEBUG", "✅ updateReplacedAmount SUCCESS: invoice=$invoiceId, total_replaced=$replacedAmount")
            }
        } catch (e: Exception) {
            Log.e("RETAILONE_DEBUG", "❌ updateReplacedAmount ERROR: ${e.message}")
        }
    }
    /**
     * Helper to get reason name by ID
     */
    suspend fun getReasonNameById(reasonId: Int): String? {
        return try {
            val database = PosDatabase.getDatabase(context)
            val reason = database.returnReasonDao().getReasonById(reasonId)
            reason?.reason_name
        } catch (e: Exception) {
            null
        }
    }
}
