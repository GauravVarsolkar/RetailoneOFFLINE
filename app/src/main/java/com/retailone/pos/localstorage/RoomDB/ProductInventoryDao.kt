package com.retailone.pos.localstorage.RoomDB

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductInventoryDao {

    // Insert or replace inventory data
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItems(items: List<ProductInventoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: ProductInventoryEntity)

    // Get all inventory for a store (reactive with Flow)
    @Query("SELECT * FROM product_inventory WHERE store_id = :storeId ORDER BY category_id, product_name ASC")
    fun getInventoryByStore(storeId: Int): Flow<List<ProductInventoryEntity>>

    // Get inventory grouped by category
    @Query("SELECT * FROM product_inventory WHERE store_id = :storeId ORDER BY category_id, product_name ASC")
    suspend fun getInventoryByStoreSync(storeId: Int): List<ProductInventoryEntity>

    // Update stock quantity
    @Query("UPDATE product_inventory SET stock_quantity = :newQuantity WHERE compositeKey = :key")
    suspend fun updateStockQuantity(key: String, newQuantity: Double)

    // Clear inventory for a store
    @Query("DELETE FROM product_inventory WHERE store_id = :storeId")
    suspend fun clearStoreInventory(storeId: Int)

    // Clear all inventory
    @Query("DELETE FROM product_inventory")
    suspend fun clearAll()

    // Get count (for debugging)
    @Query("SELECT COUNT(*) FROM product_inventory WHERE store_id = :storeId")
    suspend fun getInventoryCount(storeId: Int): Int
}
