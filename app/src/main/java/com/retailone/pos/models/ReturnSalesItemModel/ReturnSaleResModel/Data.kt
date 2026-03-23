package com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel

import com.retailone.pos.models.PosSalesDetailsModel.VsdcReceipt


data class Data(
    val returned_items: List<ReturnedItem>,

// totals & invoice
    val total: Double,
    val returned_invoice_id: String,
    val grand_total: String?, // "796.55"

// tax block
    val tax: String?, // "16.00"
    val tax_amount: String?, // "96.55"
    val tax_ex: String?, // "0.00"

// store + meta
    val store: Store,

// optional meta fields (can be empty or omitted)
    val vat_no: String?,
    val tpin_no: String?,
    val buyers_tpin: String?,
    val ej_no: String?,
    val ej_activation_date: String?,
    val sdc_id: String?,
    val receipt_no: String?,
    val internal_data: String?,
    val receipt_sign: String?,
    val rcptType: String,
    val subtal: String,

// NOTE: not present in your sample; keep it nullable if backend may send later
    val returned_date: String? = null,

// customer (nullable in payload)
    val customer_name: String?,
    val vsdc_reciept: VsdcReceipt?,
    val customer_mob_no: String?
)

/*
data class Data(
val buyers_tpin: String,
val customer_mob_no: String?,
val customer_name: String?,
val ej_activation_date: String,
val ej_no: String,
val grand_total: String,
val internal_data: String,
val receipt_no: String,
val receipt_sign: String,
val returned_date: String,
val returned_invoice_id: String,
val returned_items: List<ReturnedItem>,
val sdc_id: String,
val store: Store,
val tax: String,
val tax_amount: String,
val tax_ex: String,

val total: Double,
val tpin_no: String,
val vat_no: String
)*/