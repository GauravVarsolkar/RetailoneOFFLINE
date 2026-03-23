package com.retailone.pos.models.ReturnSalesItemModel


import com.google.gson.annotations.SerializedName

data class ReturnItemData(
    val amount_tendered: Int,
    val created_at: String,
//    val customer: Customer,
    val customer: Customer? = null,

    val discount_amount: String,
    val grand_total: String,
    val id: Int,
    val invoice_id: String,
    val payment_type: String,
    val reason_id: Int = -1,

    // Your screen uses this (camelCase). Keep as-is.
    @SerializedName("salesItems")
    val salesItems: List<SalesItem>,

    val status: Int,
    val store_details: StoreDetails,
    val store_id: Int,
    val store_manager_details: StoreManagerDetails,
    val store_manager_id: Int,
    val total_refunded_amount: Double,
    val total_replaced_amount: Double,
    val sub_total: Double,
    val subtotal_after_discount: Double,
    val tax: String,
    val tax_amount: String,
    val updated_at: String,

    // Add the snake_case version which carries tax_exclusive_price
   @SerializedName("sales_items")
   // @SerializedName("salesItems")
    val sales_items: List<SalesItemDetailed>? = null
)

// Minimal “detailed” item only for reading tax_exclusive_price
data class SalesItemDetailed(
    val id: Int,
    val sales_id: String,
    val product_id: Int,
    val on_hold: Int,
    val distribution_pack_id: Int,
    val distribution_pack_name: String,
    val batch: String?,
    val quantity: Double,
    val store_stock: Int,
    val retail_price: Double,
    val discount : Double,
    val tax_exclusive_price: Double?,   // <-- from API here
    val total_amount: Double,
    val product: Product,
    val distribution_pack: DistributionPack
)




