package com.retailone.pos.models.ReturnSalesItemModel

import com.google.gson.annotations.SerializedName


data class SalesItem(
    val created_at: String?,
    val distribution_pack: DistributionPack?,
    @SerializedName("distribution_pack_id", alternate = ["dist_pack_id"])
    val distribution_pack_id: Int,
    val distribution_pack_name: String?,
    @SerializedName("id", alternate = ["sales_item_id"])
    val id: Int,
    val product: Product?,
    @SerializedName("product_id")
    val product_id: Int,
    val product_name: String?,
    val quantity: Double,

    val batches: List<BatchReturnItem>?,

    val retail_price: Double,
    val tax_exclusive_price: Double?,
    val sales_id: String?,
    val status: Int,

    val total_amount: Double,
    val updated_at: String?,

    val whole_sale_price: Double,
    val tax: Int,
    val tax_amount: Double,

    var return_quantity: Int = 0, //manually added extra
    var refund_amount: Double = 0.0 ,//manually added extra
    var return_reason: String? = null, // added for adapter fix
    var discount: Double = 0.0,
    val readonlyMode: Boolean = false,
    val isExpired: Boolean = false
)

/*
data class SalesItem(
    val created_at: String,
    val distribution_pack: DistributionPack,
    val distribution_pack_id: Int,
    val distribution_pack_name: String,
    val id: Int,
    val product: Product,
    val product_id: Int,
    val product_name: String,
    val quantity: Double,

    val batches: List<BatchReturnItem>,

    val retail_price: Double,
    val tax_exclusive_price: Double?,
    val sales_id: String,
    val status: Int,

    val total_amount: Double,
    val updated_at: String,

    val whole_sale_price: Double,


    var return_quantity: Int = 0, //manually added extra
    var refund_amount: Double = 0.0 ,//manually added extra
    val readonlyMode: Boolean,
    val isExpired: Boolean
)*/
