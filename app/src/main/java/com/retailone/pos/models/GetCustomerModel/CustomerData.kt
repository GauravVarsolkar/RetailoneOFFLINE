package com.retailone.pos.models.GetCustomerModel

data class CustomerData(
    val address: String,
    val cin: String,
    val clusters_id: Any,
    val contact_person_name: String,
    val created_at: String,
    val customer_name: String? ="",
    val deleted_at: Any,
    val email: String,
    val id: Int,
    val kyc_document: String,
    val mobile_no: String? = "",
    val tin_tpin_no: String?="",
    val updated_at: String,
    val vat_reg_no: String
)