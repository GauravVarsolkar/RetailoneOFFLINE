package com.retailone.pos.models.PosSalesDetailsModel

data class VsdcReceipt(
    val rcptNo: String,
    val intrlData: String,
    val rcptSign: String,
    val totRcptNo: String,
    val vsdcRcptPbctDate: String,
    val sdcId: String,
    val mrcNo: String,
    val qrCodeUrl: String,
    val store_id: Int,
    val sales_id: Int?,
    val sales_type: String,
    val rec_type: String,
    val created_at: String
)
