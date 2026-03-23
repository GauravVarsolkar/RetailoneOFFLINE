package com.retailone.pos.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.retailone.pos.localstorage.RoomDB.DetailedSaleDao
import com.retailone.pos.localstorage.RoomDB.DetailedSaleEntity
import com.retailone.pos.localstorage.RoomDB.PosDatabase
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemData

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
                invoice_id = returnItemData.invoice_id,
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
     * Update total_refunded_amount for a sale (after successful return)
     */
    suspend fun updateRefundedAmount(invoiceId: String, refundedAmount: Double, reasonId: Int) {
        try {
            Log.d(TAG, "🔍 updateRefundedAmount called: invoice=$invoiceId, amount=$refundedAmount, reason=$reasonId")

            val entity = dao.getDetailedSaleByInvoiceId(invoiceId)
            if (entity != null) {
                Log.d(TAG, "✅ Found entity in DB")

                val saleData = gson.fromJson(entity.detailed_data_json, ReturnItemData::class.java)

                Log.d(TAG, "📝 BEFORE update: refunded=${saleData.total_refunded_amount}, reason=${saleData.reason_id}")

                // ✅ Update refunded amount AND reason_id
                val updatedSaleData = saleData.copy(
                    total_refunded_amount = refundedAmount,
                    reason_id = reasonId
                )

                Log.d(TAG, "📝 AFTER update: refunded=${updatedSaleData.total_refunded_amount}, reason=${updatedSaleData.reason_id}")

                // Save back to database
                val updatedEntity = entity.copy(
                    detailed_data_json = gson.toJson(updatedSaleData)
                )

                dao.insertDetailedSale(updatedEntity)
                Log.d(TAG, "✅ Saved to DB: invoice=$invoiceId, refunded=$refundedAmount, reason=$reasonId")

                // ✅ VERIFY: Read it back immediately
                kotlinx.coroutines.delay(50)
                val verifyEntity = dao.getDetailedSaleByInvoiceId(invoiceId)
                if (verifyEntity != null) {
                    val verifySale = gson.fromJson(verifyEntity.detailed_data_json, ReturnItemData::class.java)
                    Log.d(TAG, "🔍 VERIFY after save: reason_id = ${verifySale.reason_id}")
                }

            } else {
                Log.e(TAG, "❌ Entity NOT found for invoice: $invoiceId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating refunded amount and reason: ${e.message}", e)
            e.printStackTrace()
        }
    }


    /**
     * Clear all detailed sales
     */
    suspend fun clearAllDetailedSales() {
        dao.clearAll()
        Log.d(TAG, "🗑️ Cleared all detailed sales")
    }
}
