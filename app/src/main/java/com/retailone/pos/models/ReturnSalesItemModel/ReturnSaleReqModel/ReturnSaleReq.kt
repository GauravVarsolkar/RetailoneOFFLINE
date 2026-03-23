package com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel

import com.google.gson.annotations.SerializedName

/*
data class ReturnSaleReq(
    val reason_id: Int,
    val returned_items: List<ReturnedItem>,
    val sales_id: Int,
    val store_id: Int,
    val store_manager_id: Int,
    val return_date_time: String,
)*/
// ReturnedItem for returns


// Return request
data class ReturnSaleReq(
    val sales_id: Int,
    val reason_id: Int,
    val store_id: Int,
    val store_manager_id: Int,
    val returned_items: List<ReturnedItem>,
    //@SerializedName("return_date_time")
    //val return_date_time: String? = null  // <- optional, not sent if null
)
