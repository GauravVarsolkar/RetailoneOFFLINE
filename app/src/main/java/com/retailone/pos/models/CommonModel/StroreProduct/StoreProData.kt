package com.retailone.pos.models.CommonModel.StroreProduct

import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.DispatchBatchDetails

data class StoreProData(
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
    ///
    val retail_price: String?,

   // val looseoil_quantity:Double,

    var cart_quantity: Double = 0.0, // Initialize with a default value

    //var cart_quantity: Int = 0 ,//manually added extra
    var dispense_status: Int = 0, //manually added extra // 0 - packed,1- loose not dispensed 2 - loose dispensed//


    var batch: List<PosSaleBatch> = emptyList(),

    )