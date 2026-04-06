package com.retailone.pos.localstorage.RoomDB

import androidx.room.*

@Dao
interface StockReturnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockReturnList(entity: StockReturnEntity)

    @Query("SELECT * FROM stock_return_list WHERE store_id = :storeId")
    suspend fun getStockReturnListByStore(storeId: Int): StockReturnEntity?

    @Query("DELETE FROM stock_return_list")
    suspend fun clearAll()
}
