package com.retailone.pos.models.ExpenseRegisterModel.ExpenseHistory

data class ExpenseCategory(
    val category_name: String,
    val created_at: String,
    val id: Int,
    val status: Int,
    val updated_at: String
)