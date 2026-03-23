package com.retailone.pos.models.PrinterModel

import com.retailone.pos.models.PosSalesDetailsModel.SalesItem

data class ReceiptData(
    val top: String,
    val storeName: String,
    val storeAddress: String,
    val vatInfo: String,
    val dateTime: String,
    val receiptInfo: String,
    val itemInfo:String,
    val totalPrice: String,
    val itemCount: String,
    val taxInfo: String,
    val totalVat: String,
    val payableAmount: String,
    val paymentModeInfo: String,
    val ejInfo: String,
    val bottom: String,
    val product_item:  List<SalesItem>,
)
