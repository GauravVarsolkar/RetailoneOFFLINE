package com.retailone.pos.repository

import android.content.Context
import android.util.Log
import com.retailone.pos.localstorage.RoomDB.Converters
import com.retailone.pos.localstorage.RoomDB.PosDatabase
import com.retailone.pos.localstorage.RoomDB.StoreProductDao
import com.retailone.pos.localstorage.RoomDB.toEntity
import com.retailone.pos.localstorage.RoomDB.toStoreProData
import com.retailone.pos.localstorage.RoomDB.CustomerDiscountEntity
import com.retailone.pos.models.CommonModel.StroreProduct.StoreProData
import com.retailone.pos.models.PointofsaleModel.SearchStroreProModel.SearchStoreProReq
import com.retailone.pos.models.PointofsaleModel.SearchStroreProModel.SearchStoreProRes
import com.retailone.pos.network.ApiClient
import com.retailone.pos.utils.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.launch

class PosProductRepository(private val context: Context) {

    private val database = PosDatabase.getDatabase(context)
    private val dao: StoreProductDao = database.storeProductDao()

    /**
     * Offline-first search:
     * - Always return Flow from Room (UI subscribes to this)
     * - If online, fetch from API and update Room in background
     * - If offline, just return cached data
     */
    fun searchProducts(
        searchQuery: String,
        storeId: Int,
        customerId: Int,
        onError: (String) -> Unit
    ): Flow<List<StoreProData>> {

        // Return Flow from Room (this updates UI automatically)
        val localFlow = if (searchQuery.isEmpty()) {
            dao.getProductsByStore(storeId)
        } else {
            dao.searchProducts(storeId, searchQuery)
        }.map { entities ->
            val products = entities.map { it.toStoreProData() }
            if (customerId != 0) {
                products.forEach { product ->
                    product.batch.forEach { batch ->
                        val offlineDiscount = database.customerDiscountDao().getDiscount(
                            customerId,
                            product.product_id,
                            product.distribution_pack_id,
                            batch.batch_no
                        )
                        batch.discount = offlineDiscount ?: 0.0
                    }
                }
            } else {
                products.forEach { product ->
                    product.batch.forEach { batch ->
                        batch.discount = 0.0
                    }
                }
            }
            products
        }

        // If online, sync with API in background
        if (NetworkUtils.isInternetAvailable(context)) {
            fetchAndCacheFromApi(searchQuery, storeId, customerId, onError)
        }

        return localFlow
    }

    /**
     * Fetch from API and save to Room
     */
    private fun fetchAndCacheFromApi(
        searchQuery: String,
        storeId: Int,
        customerId: Int,
        onError: (String) -> Unit
    ) {
        ApiClient().getApiService(context)
            .searchStoreProduct(SearchStoreProReq(searchQuery, storeId, customerId))
            .enqueue(object : Callback<SearchStoreProRes> {

                override fun onResponse(
                    call: Call<SearchStoreProRes>,
                    response: Response<SearchStoreProRes>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val products = response.body()!!.data

                        // Save to Room (background thread)
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            try {
                                val entities = products.map { it.toEntity() }
                                dao.insertProducts(entities)
                                
                                // Save discounts per customer
                                val discounts = mutableListOf<CustomerDiscountEntity>()
                                products.forEach { product ->
                                    product.batch.forEach { batch ->
                                        discounts.add(
                                            CustomerDiscountEntity(
                                                customer_id = customerId,
                                                product_id = product.product_id,
                                                distribution_pack_id = product.distribution_pack_id,
                                                batch_no = batch.batch_no,
                                                discount = batch.discount
                                            )
                                        )
                                    }
                                }
                                database.customerDiscountDao().insertDiscounts(discounts)
                                
                                Log.d("PosRepository", "Cached ${entities.size} products and ${discounts.size} discounts for customer $customerId")
                            } catch (e: Exception) {
                                Log.e("PosRepository", "Error caching products: ${e.message}")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<SearchStoreProRes>, t: Throwable) {
                    Log.e("PosRepository", "API call failed: ${t.message}")
                    onError("Failed to sync with server")
                }
            })
    }

    /**
     * Search by barcode
     */
    suspend fun getProductByBarcode(barcode: String, storeId: Int): StoreProData? {
        val entity = dao.getProductByBarcode(barcode, storeId)
        return entity?.toStoreProData()
    }

    /**
     * Update cart quantity in Room
     */
    suspend fun updateCartQuantity(productId: Int, distributionPackId: Int, quantity: Double) {
        val key = "${productId}_${distributionPackId}"
        dao.updateCartQuantity(key, quantity)
    }

    /**
     * Update batch data in Room
     */
    suspend fun updateBatchData(product: StoreProData) {
        val entity = product.toEntity()
        dao.insertProduct(entity) // REPLACE strategy updates existing
    }

    /**
     * Clear cache for a store
     */
    suspend fun clearStoreCache(storeId: Int) {
        dao.clearStoreProducts(storeId)
    }

    /**
     * Get cached product count (for debugging)
     */
    suspend fun getCachedProductCount(storeId: Int): Int {
        return dao.getProductCount(storeId)
    }
    /**
     * Deduct stock quantities after a sale (offline mode)
     * This updates both stock_quantity and batch quantities in local DB
     */
    suspend fun deductStockAfterSale(cartItems: List<StoreProData>) {
        val stockUpdates = mutableMapOf<String, Double>()

        cartItems.forEach { item ->
            val key = "${item.product_id}_${item.distribution_pack_id}"

            // Calculate total quantity sold for this product (sum of all batches)
            val totalQtySold = item.batch.sumOf { it.batch_cart_quantity }

            if (totalQtySold > 0) {
                // Get the current product from database
                val currentProduct = dao.getProductByKey(key)

                if (currentProduct != null) {
                    val currentBatches = Converters().fromBatchJson(currentProduct.batchJson)

                    // Update batch quantities by matching batch_no
                    val updatedBatches = currentBatches.map { dbBatch ->
                        val soldBatch = item.batch.find { it.batch_no == dbBatch.batch_no }
                        if (soldBatch != null && soldBatch.batch_cart_quantity > 0) {
                            // Deduct the sold quantity from this batch
                            dbBatch.copy(
                                quantity = (dbBatch.quantity - soldBatch.batch_cart_quantity).coerceAtLeast(0.0)
                            )
                        } else {
                            dbBatch
                        }
                    }

                    // Calculate new total stock quantity
                    val newStockQty = updatedBatches.sumOf { it.quantity }

                    // Update the product entity with new batch data and stock quantity
                    val updatedEntity = currentProduct.copy(
                        stock_quantity = newStockQty,
                        batchJson = Converters().toBatchJson(updatedBatches),
                        cart_quantity = 0.0
                    )

                    // Save updated product to database
                    dao.insertProduct(updatedEntity)

                    Log.d("PosProductRepo", "✅ Deducted stock for ${item.product_name}: ${totalQtySold} units. New stock: $newStockQty")
                }
            }
        }
    }


}
