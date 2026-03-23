package com.retailone.pos.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.retailone.pos.localstorage.RoomDB.CompletedSaleDao
import com.retailone.pos.localstorage.RoomDB.CompletedSaleEntity
import com.retailone.pos.localstorage.RoomDB.PosDatabase
import com.retailone.pos.models.SalesData
import kotlinx.coroutines.flow.Flow

class CompletedSaleRepository(private val context: Context) {

    private val database = PosDatabase.getDatabase(context)
    private val dao: CompletedSaleDao = database.completedSaleDao()
    private val gson = Gson()

    companion object {
        private const val TAG = "CompletedSaleRepo"
        private const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1000
    }

    /**
     * Save sales from API to local database
     */
    suspend fun saveSalesFromList(salesList: List<SalesData>) {
        // ✅ First, clear existing sales to avoid duplicates
        dao.clearAll()

        val entities = salesList.map { salesData ->
            CompletedSaleEntity(
                invoice_id = salesData.invoice_id,
                store_id = salesData.store_id,
                sale_id = salesData.id,
                sale_data_json = gson.toJson(salesData)
            )
        }

        // ✅ Insert fresh sales list
        dao.insertSales(entities)
        Log.d(TAG, "💾 Saved ${entities.size} sales (replaced old data)")
    }


    /**
     * Get all sales as Flow (for RecyclerView)
     */
    fun getAllSalesFlow(): Flow<List<CompletedSaleEntity>> {
        return dao.getAllSalesFlow()
    }

    /**
     * Search sale by invoice ID
     */
    suspend fun getSaleByInvoiceId(invoiceId: String): SalesData? {
        val entity = dao.getSaleByInvoiceId(invoiceId) ?: return null
        return try {
            gson.fromJson(entity.sale_data_json, SalesData::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing sale data: ${e.message}")
            null
        }
    }

    /**
     * Convert entity list to SalesData list (for displaying)
     */
    fun entitiesToSalesDataList(entities: List<CompletedSaleEntity>): List<SalesData> {
        return entities.mapNotNull { entity ->
            try {
                gson.fromJson(entity.sale_data_json, SalesData::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing sale: ${e.message}")
                null
            }
        }
    }

    /**
     * Delete sales older than 7 days
     */
    suspend fun deleteOldSales(): Int {
        val sevenDaysAgo = System.currentTimeMillis() - SEVEN_DAYS_MILLIS
        val deletedCount = dao.deleteSalesOlderThan(sevenDaysAgo)
        Log.d(TAG, "🗑️ Deleted $deletedCount old sales")
        return deletedCount
    }

    /**
     * Get sales count
     */
    suspend fun getSalesCount(): Int {
        return dao.getSalesCount()
    }
}
