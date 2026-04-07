package com.retailone.pos.models.PointofsaleModel.PosSaleModel

import com.google.gson.annotations.SerializedName
import com.retailone.pos.models.PosSalesDetailsModel.TaxDetails
import com.retailone.pos.models.PosSalesDetailsModel.TaxSummary


data class PosSaleReq(
    val customer_mob_no: String,
    val customer_name: String,
    val customer_id: Int,
    val discount_amount: String,
    val grand_total: String,
    val payment_type: String,
    val sales_items: List<PosSalesItem>,
    val sub_total: String,
    val subtotal_after_discount: String,
    val tax: String,
    val tax_amount: String,
    val store_id:String,
    val store_manager_id:String,
    val amount_tendered:String,
    val sale_date_time:String,
    val tin_tpin_no: String,
    val invoice_id  : String,
    val prc_no: String? = null,
    val trxn_code: String,
    val tax_details: TaxDetails?,
    val total_after_discount: Int,
    val tax_summery: List<TaxSummary>?,
    val discount_rate : Int,
    val spot_discount_percentage: Double,
    val spot_discount_amount: String,
)

