package com.retailone.pos.models.ExpenseRegisterModel.ExpenseHistory

data class ExpenseHistoryData(
    val amount: Int,
    val total_amount: String,
    val category: ExpenseCategory?,
    val category_id: Int,
    val created_at: String,
    val id: Int,
    val invoice: String,
    val status: Int,
    val store_manager_id: Int,
    val updated_at: String,
    val vendor: ExpenseVendor?,
    val vendor_id: String
)