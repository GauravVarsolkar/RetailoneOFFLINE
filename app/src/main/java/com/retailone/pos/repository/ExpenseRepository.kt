package com.retailone.pos.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.retailone.pos.localstorage.RoomDB.PendingExpenseDao
import com.retailone.pos.localstorage.RoomDB.PendingExpenseEntity
import com.retailone.pos.localstorage.RoomDB.PosDatabase
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseSubmit.ExpenseSubmitReq
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseSubmit.ExpenseSubmitRes
import com.retailone.pos.network.ApiClient
import com.retailone.pos.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.Response

class ExpenseRepository(private val context: Context) {

    private val database = PosDatabase.getDatabase(context)
    private val dao: PendingExpenseDao = database.pendingExpenseDao()
    private val apiService = ApiClient().getApiService(context)

    companion object {
        private const val TAG = "ExpenseRepository"
    }

    /**
     * Submit expense (offline-first):
     * - If ONLINE: Try API first, save to Room only if API fails
     * - If OFFLINE: Save locally as PENDING
     */
    suspend fun submitExpense(
        expenseReq: ExpenseSubmitReq,
        onSuccess: (ExpenseSubmitRes) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isInternetAvailable(context)) {
            // ✅ ONLINE: Try API first
            submitToApiWithFallback(expenseReq, onSuccess, onError)
        } else {
            // ✅ OFFLINE: Save locally
            saveExpenseLocally(expenseReq)
            onSuccess(createOfflineSuccessResponse(expenseReq))
        }
    }

    /**
     * Try API, if it fails save locally
     */
    private suspend fun submitToApiWithFallback(
        expenseReq: ExpenseSubmitReq,
        onSuccess: (ExpenseSubmitRes) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val response = apiService.getExpenseSubmitAPI(expenseReq).execute()

            if (response.isSuccessful && response.body() != null) {
                // ✅ API success - save locally as SYNCED for history
                saveExpenseLocally(expenseReq, syncStatus = "SYNCED")
                onSuccess(response.body()!!)
            } else {
                // ❌ API failed (internal error), save locally as PENDING
                saveExpenseLocally(expenseReq)
                onSuccess(createOfflineSuccessResponse(expenseReq))
            }
        } catch (e: Exception) {
            // ❌ Network error, save locally as PENDING
            Log.e(TAG, "submitToApiWithFallback error: ${e.message}")
            saveExpenseLocally(expenseReq)
            onSuccess(createOfflineSuccessResponse(expenseReq))
        }
    }

    /**
     * Save expense to Room database
     */
    private suspend fun saveExpenseLocally(expenseReq: ExpenseSubmitReq, syncStatus: String = "PENDING") {
        try {
            val entity = PendingExpenseEntity(
                amount = expenseReq.amount,
                category_name = expenseReq.category_name,
                invoice = expenseReq.invoice,
                store_manager_id = expenseReq.store_manager_id,
                expense_date_time = expenseReq.expense_date_time,
                store_id = expenseReq.store_id,
                vendor_name = expenseReq.vendor_name,
                vat = expenseReq.vat,
                total_amount = expenseReq.total_amount,
                sdc_no = expenseReq.sdc_no,
                receipt_no = expenseReq.receipt_no,
                remarks = expenseReq.remarks,
                sync_status = syncStatus
            )

            val id = dao.insertPendingExpense(entity)
            Log.d(TAG, "Expense saved locally with ID: $id, Status: $syncStatus")

        } catch (e: Exception) {
            Log.e(TAG, "Error saving expense locally: ${e.message}")
        }
    }

    /**
     * Create a success response for offline expenses
     */
    private fun createOfflineSuccessResponse(expenseReq: ExpenseSubmitReq): ExpenseSubmitRes {
        return ExpenseSubmitRes(
            status = 1,
            message = "Expense saved offline. Will sync when online.",
            data = 0
        )
    }

    /**
     * Get pending expenses count as Flow (for UI badges)
     */
    fun getPendingExpensesCountFlow(): Flow<Int> {
        return dao.getPendingExpensesCountFlow()
    }

    /**
     * Sync all offline expenses to server
     */
    suspend fun syncAllPendingExpenses(): Boolean = withContext(Dispatchers.IO) {
        try {
            val pendingExpenses = dao.getPendingExpenses()

            if (pendingExpenses.isEmpty()) {
                Log.d(TAG, "No offline expenses to sync")
                return@withContext true
            }

            Log.d(TAG, "Found ${pendingExpenses.size} offline expenses to sync")
            var failCount = 0

            for (expense in pendingExpenses) {
                val syncResult = syncSingleExpense(expense)
                if (!syncResult) failCount++
            }

            return@withContext failCount == 0
        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}")
            return@withContext false
        }
    }

    private suspend fun syncSingleExpense(expense: PendingExpenseEntity): Boolean {
        try {
            dao.updateSyncStatus(expense.id, "SYNCING", System.currentTimeMillis())

            val expenseReq = ExpenseSubmitReq(
                amount = expense.amount,
                category_name = expense.category_name,
                invoice = expense.invoice,
                store_manager_id = expense.store_manager_id,
                expense_date_time = expense.expense_date_time,
                store_id = expense.store_id,
                vendor_name = expense.vendor_name,
                vat = expense.vat,
                total_amount = expense.total_amount,
                sdc_no = expense.sdc_no,
                receipt_no = expense.receipt_no,
                remarks = expense.remarks
            )

            val response = apiService.getExpenseSubmitAPI(expenseReq).execute()

            return if (response.isSuccessful) {
                dao.markAsSynced(expense.id, System.currentTimeMillis())
                Log.d(TAG, "Expense ${expense.id} synced successfully")
                true
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                dao.updateSyncStatusWithError(expense.id, "FAILED", System.currentTimeMillis(), errorMsg)
                Log.e(TAG, "Expense ${expense.id} sync failed: $errorMsg")
                false
            }
        } catch (e: Exception) {
            dao.updateSyncStatusWithError(expense.id, "FAILED", System.currentTimeMillis(), e.message ?: "Unknown error")
            Log.e(TAG, "Expense ${expense.id} sync error: ${e.message}")
            return false
        }
    }
}
