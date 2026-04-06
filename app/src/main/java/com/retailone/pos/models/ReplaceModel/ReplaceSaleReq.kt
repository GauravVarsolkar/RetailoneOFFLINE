package com.retailone.pos.models.ReplaceModel



import com.google.gson.annotations.SerializedName

data class ReplaceReturnedItem(
    val id: Int,
    @SerializedName("return_quantity") val return_quantity: Int,
    // Optional – only needed for “Incomplete Box”
    @SerializedName("on_hold") val on_hold : Int,
    val defective_boxes: Int? = null,
    val defective_bottles: Int? = null,
    val product_id: Int? = null,
    val distribution_pack_id: Int? = null
)

data class ReplaceSaleReq(
    val sales_id: Int,
    val reason_id: Int,
    val store_id: Int,
    val store_manager_id: Int,
    val on_hold: Int,
    val remark: String,// 0 = Save & Replace, 1 = HOLD
    @SerializedName("return_date_time")
    val return_date_time: String,
    @SerializedName("returned_items")
    val returned_items: List<ReplaceReturnedItem>
)
