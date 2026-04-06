package com.retailone.pos.localstorage.RoomDB

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CustomerDiscountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscounts(discounts: List<CustomerDiscountEntity>)

    @Query("SELECT discount FROM customer_discounts WHERE customer_id = :customerId AND product_id = :productId AND distribution_pack_id = :packId AND batch_no = :batchNo")
    suspend fun getDiscount(customerId: Int, productId: Int, packId: Int, batchNo: String): Double?
}
