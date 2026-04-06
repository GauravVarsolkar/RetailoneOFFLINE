package com.retailone.pos.models.CommonModel.StroreProduct

import com.google.gson.annotations.SerializedName


data class PosSaleBatch (
    val batch_no: String,
    val quantity: Double,
    val price: Double,
    val tax: String? = null, // Tax rate from API (e.g., "0", "18")
    var batch_cart_quantity: Double = 0.0,
    var batch_total_du_amount: String = "",
    var dispense_status: Int = 0, //manually added extra // 0 - packed,1- loose not dispensed 2 - loose dispensed//
    @SerializedName("discount")
    var discount: Double = 0.0
)