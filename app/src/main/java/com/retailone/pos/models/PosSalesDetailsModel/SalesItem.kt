package com.retailone.pos.models.PosSalesDetailsModel

data class SalesItem(
    val distribution_pack: DistributionPack?,
    val distribution_pack_id: String?,
    val distribution_pack_name: String?,
    val product_id: String?,
    val product_name: String?,
    //val quantity: Double,
    val quantity: String?,
    val retail_price: String?,
    val tax_inclusive_price: String,
    val discount: Int,
    val tax: Int,
    // val total_amount: String,
    val total_amount: Double?,
    val whole_sale_price: String?,

    val uom: String?,
    val batchno: String?,

    // printer fields
    val tax_details: TaxDetails? = null,
    val discount_rate: Double? = null
)