package com.retailone.pos.models.PosSalesDetailsModel


data class TaxSummary(
    val code: String?,
    val rate: Int?,
    val taxable_value: Double?,
    val tax_amount: Double?,
    val gross_total: Int?,
    val code_name: String?
)

data class TaxDetails(
    val id: Int?,
    val name: String?,
    val amount: String?,
    val type: String?,
    val organization_id: Int?,
    val deleted_at: String?,
    val status: Int?,
    val created_at: String?,
    val updated_at: String?,
    val code: String?
)