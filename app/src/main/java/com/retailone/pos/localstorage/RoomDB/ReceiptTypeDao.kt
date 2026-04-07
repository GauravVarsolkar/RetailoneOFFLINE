package com.retailone.pos.localstorage.RoomDB

import androidx.room.*

@Dao
interface ReceiptTypeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceiptTypes(types: List<ReceiptTypeEntity>)

    @Query("SELECT * FROM receipt_types")
    suspend fun getAllReceiptTypes(): List<ReceiptTypeEntity>

    @Query("DELETE FROM receipt_types")
    suspend fun clearAll()
}
