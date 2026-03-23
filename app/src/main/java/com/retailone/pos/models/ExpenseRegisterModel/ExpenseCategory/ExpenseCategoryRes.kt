package com.retailone.pos.models.ExpenseRegisterModel.ExpenseCategory

data class ExpenseCategoryRes(
    val `data`: List<ExpenseCategoryData>,
    val message: String,
    val status: Int
)