package com.retailone.pos.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.retailone.pos.localstorage.RoomDB.PosDatabase
import com.retailone.pos.localstorage.RoomDB.SalesDetailsDao
import com.retailone.pos.localstorage.RoomDB.SalesDetailsEntity
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.SalesDetailsRes

class SalesDetailsRepository(private val context: Context) {

    private val database = PosDatabase.getDatabase(context)
    private val dao: SalesDetailsDao = database.salesDetailsDao()
    private val gson = Gson()

    companion object {
        private const val TAG = "SalesDetailsRepo"
        private const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1000
    }

    /**
     * Save sales details to local database
     */
    suspend fun saveSalesDetails(saleId: Int, invoiceId: String, salesDetailsRes: SalesDetailsRes) {
        try {
            val entity = SalesDetailsEntity(
                sale_id = saleId,
                invoice_id = invoiceId,
                sales_details_json = gson.toJson(salesDetailsRes)
            )

            dao.insertSalesDetails(entity)
            Log.d(TAG, "✅ Saved sales details: sale_id=$saleId, invoice_id=$invoiceId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving sales details: ${e.message}", e)
        }
    }

    /**
     * Save multiple sales details at once
     */
    suspend fun saveMultipleSalesDetails(salesDetailsList: List<Triple<Int, String, SalesDetailsRes>>) {
        try {
            val entities = salesDetailsList.map { (saleId, invoiceId, salesDetailsRes) ->
                SalesDetailsEntity(
                    sale_id = saleId,
                    invoice_id = invoiceId,
                    sales_details_json = gson.toJson(salesDetailsRes)
                )
            }

            dao.insertAllSalesDetails(entities)
            Log.d(TAG, "✅ Saved ${entities.size} sales details in batch")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving multiple sales details: ${e.message}", e)
        }
    }

    /**
     * Get sales details by sale ID (for offline viewing)
     */
    suspend fun getSalesDetailsBySaleId(saleId: Int): SalesDetailsRes? {
        return try {
            val entity = dao.getSalesDetailsBySaleId(saleId)
            if (entity != null) {
                gson.fromJson(entity.sales_details_json, SalesDetailsRes::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error retrieving sales details by sale_id: ${e.message}", e)
            null
        }
    }

    /**
     * Get sales details by invoice ID (for offline viewing)
     */
    suspend fun getSalesDetailsByInvoiceId(invoiceId: String): SalesDetailsRes? {
        return try {
            val entity = dao.getSalesDetailsByInvoiceId(invoiceId)
            if (entity != null) {
                gson.fromJson(entity.sales_details_json, SalesDetailsRes::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error retrieving sales details by invoice_id: ${e.message}", e)
            null
        }
    }

    /**
     * Check if sales details exists
     */
    suspend fun salesDetailsExists(saleId: Int): Boolean {
        return dao.salesDetailsExists(saleId)
    }

    /**
     * Check if sales details exists by invoice ID
     */
    suspend fun salesDetailsExistsByInvoiceId(invoiceId: String): Boolean {
        return dao.salesDetailsExistsByInvoiceId(invoiceId)
    }

    /**
     * Delete sales details older than 7 days
     */
    suspend fun deleteOldSalesDetails(): Int {
        val sevenDaysAgo = System.currentTimeMillis() - SEVEN_DAYS_MILLIS
        val deletedCount = dao.deleteSalesDetailsOlderThan(sevenDaysAgo)
        Log.d(TAG, "🗑️ Deleted $deletedCount old sales details")
        return deletedCount
    }

    /**
     * Delete sales details by sale ID
     */
    suspend fun deleteSalesDetailsBySaleId(saleId: Int) {
        dao.deleteSalesDetailsBySaleId(saleId)
        Log.d(TAG, "🗑️ Deleted sales details for sale_id=$saleId")
    }

    /**
     * Clear all sales details
     */
    suspend fun clearAllSalesDetails() {
        dao.clearAll()
        Log.d(TAG, "🗑️ Cleared all sales details")
    }
}
