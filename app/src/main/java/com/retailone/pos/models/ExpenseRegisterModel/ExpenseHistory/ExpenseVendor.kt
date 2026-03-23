package com.retailone.pos.models.ExpenseRegisterModel.ExpenseHistory

data class ExpenseVendor(
    val created_at: String,
    val id: Int,
    val status: Int,
    val updated_at: String,
    val vendor_name: String
)