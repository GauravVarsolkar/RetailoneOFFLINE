package com.retailone.pos.localstorage.RoomDB

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "product_inventory")
@TypeConverters(Converters::class)
data class ProductInventoryEntity(
    @PrimaryKey(autoGenerate = false)
    val compositeKey: String, // Format: "storeId_categoryId_productId_distributionPackId"

    val store_id: Int,
    val category_id: String,
    val category_name: String,
    val product_id: String,
    val product_name: String,
    val product_photo: String,
    val distribution_pack_id: String,
    val no_of_packs: Int,
    val stock_quantity: Double,
    val pack_description: String,
    val retail_price: String?,
    val expiry_date: String?,
    val batch_no: String?,

    // Store returned items as JSON strings
    val returned_items_json: String?, // Map<String, ReturnedItemDetails> as JSON
    val good_returned_items_json: String?, // Map<String, ReturnedItemDetails> as JSON

    val lastUpdated: Long = System.currentTimeMillis()
)
