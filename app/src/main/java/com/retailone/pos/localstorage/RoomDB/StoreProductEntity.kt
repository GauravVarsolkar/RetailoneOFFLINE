package com.retailone.pos.localstorage.RoomDB

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.retailone.pos.models.CommonModel.StroreProduct.PosSaleBatch
import com.retailone.pos.models.CommonModel.StroreProduct.StoreProData

@Entity(tableName = "store_products")
@TypeConverters(Converters::class)
data class StoreProductEntity(
    // Composite key = product_id + distribution_pack_id
    @PrimaryKey(autoGenerate = false)
    val compositeKey: String, // Format: "productId_distributionPackId"

    val barcode: String?,
    val category_id: Int,
    val distribution_pack_id: Int,
    val no_of_packs: Int,
    val pack_product_description: String,
    val product_description: String,
    val product_id: Int,
    val product_name: String,
    val product_photo: String,
    val quantity: Int?,
    val tax: Int?,
    val stock_quantity: Double,
    val store_id: Int,
    val supplier_id: Int,
    val uom: String?,
    val whole_sale_price: String?,
    val retail_price: String?,
    val cart_quantity: Double,
    val dispense_status: Int,

    // Store batch list as JSON string
    val batchJson: String, // Will be converted to/from List<PosSaleBatch>

    // Timestamp for cache invalidation (optional)
    val lastUpdated: Long = System.currentTimeMillis()
)

// Extension functions to convert between Entity and Domain model
fun StoreProductEntity.toStoreProData(): StoreProData {
    val batches = Converters().fromBatchJson(this.batchJson)
    return StoreProData(
        barcode = this.barcode,
        category_id = this.category_id,
        distribution_pack_id = this.distribution_pack_id,
        no_of_packs = this.no_of_packs,
        pack_product_description = this.pack_product_description,
        product_description = this.product_description,
        product_id = this.product_id,
        product_name = this.product_name,
        product_photo = this.product_photo,
        quantity = this.quantity,
        tax = this.tax,
        stock_quantity = this.stock_quantity,
        store_id = this.store_id,
        supplier_id = this.supplier_id,
        uom = this.uom,
        whole_sale_price = this.whole_sale_price,
        retail_price = this.retail_price,
        cart_quantity = this.cart_quantity,
        dispense_status = this.dispense_status,
        batch = batches
    )
}

fun StoreProData.toEntity(): StoreProductEntity {
    val compositeKey = "${this.product_id}_${this.distribution_pack_id}"
    val batchJson = Converters().toBatchJson(this.batch)
    return StoreProductEntity(
        compositeKey = compositeKey,
        barcode = this.barcode,
        category_id = this.category_id,
        distribution_pack_id = this.distribution_pack_id,
        no_of_packs = this.no_of_packs,
        pack_product_description = this.pack_product_description,
        product_description = this.product_description,
        product_id = this.product_id,
        product_name = this.product_name,
        product_photo = this.product_photo,
        quantity = this.quantity,
        tax = this.tax,
        stock_quantity = this.stock_quantity,
        store_id = this.store_id,
        supplier_id = this.supplier_id,
        uom = this.uom,
        whole_sale_price = this.whole_sale_price,
        retail_price = this.retail_price,
        cart_quantity = this.cart_quantity,
        dispense_status = this.dispense_status,
        batchJson = batchJson
    )
}
