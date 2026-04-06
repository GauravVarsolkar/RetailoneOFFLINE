package com.retailone.pos.localstorage.RoomDB

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreProductDao {

    // Insert or replace products (for caching search results)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<StoreProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: StoreProductEntity)

    // Get all products for a specific store (reactive with Flow)
    @Query("SELECT * FROM store_products WHERE store_id = :storeId ORDER BY product_name ASC")
    fun getProductsByStore(storeId: Int): Flow<List<StoreProductEntity>>

    // Search products by name or description (reactive)
    @Query("""
        SELECT * FROM store_products 
        WHERE store_id = :storeId 
        AND (
            product_name LIKE '%' || :searchQuery || '%' 
            OR pack_product_description LIKE '%' || :searchQuery || '%'
            OR barcode LIKE '%' || :searchQuery || '%'
        )
        ORDER BY product_name ASC
    """)
    fun searchProducts(storeId: Int, searchQuery: String): Flow<List<StoreProductEntity>>

    // Get single product by composite key
    @Query("SELECT * FROM store_products WHERE compositeKey = :key")
    suspend fun getProductByKey(key: String): StoreProductEntity?

    // Get product by barcode
    @Query("SELECT * FROM store_products WHERE barcode = :barcode AND store_id = :storeId LIMIT 1")
    suspend fun getProductByBarcode(barcode: String, storeId: Int): StoreProductEntity?

    // Update cart quantity for a product
    @Query("UPDATE store_products SET cart_quantity = :quantity WHERE compositeKey = :key")
    suspend fun updateCartQuantity(key: String, quantity: Double)

    // Update batch JSON (when batch quantities change)
    @Query("UPDATE store_products SET batchJson = :batchJson WHERE compositeKey = :key")
    suspend fun updateBatchJson(key: String, batchJson: String)

    // Update dispense status
    @Query("UPDATE store_products SET dispense_status = :status WHERE compositeKey = :key")
    suspend fun updateDispenseStatus(key: String, status: Int)

    // Delete product
    @Delete
    suspend fun deleteProduct(product: StoreProductEntity)

    // Clear all products for a store
    @Query("DELETE FROM store_products WHERE store_id = :storeId")
    suspend fun clearStoreProducts(storeId: Int)

    // Clear all products
    @Query("DELETE FROM store_products")
    suspend fun clearAll()

    // Get count of cached products (for debugging)
    @Query("SELECT COUNT(*) FROM store_products WHERE store_id = :storeId")
    suspend fun getProductCount(storeId: Int): Int

    // Get offline stock for a set of product IDs (used by offline replace logic)
    @Query("SELECT * FROM store_products WHERE product_id IN (:productIds) AND store_id = :storeId")
    suspend fun getProductsByProductIds(productIds: List<Int>, storeId: Int): List<StoreProductEntity>


    // Update stock quantity after sale
    @Query("UPDATE store_products SET stock_quantity = :newQuantity WHERE compositeKey = :key")
    suspend fun updateStockQuantity(key: String, newQuantity: Double)

    // Deduct stock for multiple products in a transaction
    @Transaction
    suspend fun deductStockForSale(stockUpdates: Map<String, Double>) {
        stockUpdates.forEach { (key, quantityToDeduct) ->
            val product = getProductByKey(key)
            if (product != null) {
                val newStock = (product.stock_quantity - quantityToDeduct).coerceAtLeast(0.0)
                updateStockQuantity(key, newStock)
            }
        }
    }
}
