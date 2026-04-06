package com.retailone.pos.localstorage.RoomDB

import androidx.room.*

@Dao
interface StockListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockList(entity: StockListEntity)

    @Query("SELECT * FROM warehouse_stock_list WHERE store_id = :storeId")
    suspend fun getStockListByStore(storeId: Int): StockListEntity?

    @Query("DELETE FROM warehouse_stock_list")
    suspend fun clearAll()
}
