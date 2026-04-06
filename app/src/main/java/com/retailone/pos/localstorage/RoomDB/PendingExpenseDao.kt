package com.retailone.pos.localstorage.RoomDB

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingExpense(expense: PendingExpenseEntity): Long

    @Query("SELECT * FROM pending_expenses WHERE sync_status = 'PENDING' OR sync_status = 'FAILED' ORDER BY created_at ASC")
    suspend fun getPendingExpenses(): List<PendingExpenseEntity>

    @Query("UPDATE pending_expenses SET sync_status = :status, last_sync_attempt = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, status: String, timestamp: Long)

    @Query("UPDATE pending_expenses SET sync_status = :status, last_sync_attempt = :timestamp, error_message = :error WHERE id = :id")
    suspend fun updateSyncStatusWithError(id: Int, status: String, timestamp: Long, error: String)

    @Query("UPDATE pending_expenses SET sync_status = 'SYNCED', synced_at = :timestamp WHERE id = :id")
    suspend fun markAsSynced(id: Int, timestamp: Long)

    @Query("SELECT COUNT(*) FROM pending_expenses WHERE sync_status = 'PENDING' OR sync_status = 'FAILED'")
    fun getPendingExpensesCountFlow(): Flow<Int>

    @Query("DELETE FROM pending_expenses WHERE sync_status = 'SYNCED' AND synced_at < :timestamp")
    suspend fun deleteOldSyncedExpenses(timestamp: Long)
}
