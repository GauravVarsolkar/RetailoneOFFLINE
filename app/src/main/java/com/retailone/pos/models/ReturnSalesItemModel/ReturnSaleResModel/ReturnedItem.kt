package com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel

import com.retailone.pos.models.PosSalesDetailsModel.TaxDetails

data class ReturnedItem(
    val id: Int,
    val sales_id: String,
    val product_id: Int,
    val product_name: String,
    val distribution_pack_id: Int,
    val distribution_pack_name: String,

    val quantity: Double,
    val whole_sale_price: Double,
    val retail_price: Double,
    val total_amount: Double,

    // present in response (nullable)
    val batch: String?,

    // accounting fields (present in your sample)
    val sub_total: Double?,              // 603.44827586207
    val tax_amount: Double?,             // 96.551724137931
    val tax_inclusive_price: Double?,    // 21.551724137931

    val return_quantity: Int,
    val total_returned_amount: Double,

    // audit
    val created_at: String,
    val updated_at: String,
    val status: Int,

    // may be null
    val sales_return_id: Int?,

    // printer fields
    val tax_details: TaxDetails? = null,
    val discount: Double? = null
)
