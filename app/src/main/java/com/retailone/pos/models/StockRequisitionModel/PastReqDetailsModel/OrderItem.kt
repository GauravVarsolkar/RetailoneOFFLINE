package com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel

data class OrderItem(
    val approved_quantity: Int,
    val category_details: CategoryDetails,
    val category_id: String,
    val created_at: String,
    val distribution_pack_details: DistributionPackDetails,
    val distribution_pack_id: String,
    val whole_sale_price: String,
    val retail_price: String,
    val id: Int,
    val pi_id: String,
    val product_details: ProductDetails,
    val product_id: String,
    val quantity_request: Int,
    val dispatch_qty: Int,
    val expiry_date: String,
    //val batch_no: String,
    val batch_no: List<DispatchBatchDetails>,
    val received_quantity: Int,
    val remark: Any,
    val status: String,
    val updated_at: String
)