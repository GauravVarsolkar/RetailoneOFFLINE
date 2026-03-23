package com.retailone.pos.models.ExpenseRegisterModel.ExpenseHistory

data class ExpenseHistoryRes(
    val `data`: List<ExpenseHistoryData>,
    val message: String,
    val status: String
)