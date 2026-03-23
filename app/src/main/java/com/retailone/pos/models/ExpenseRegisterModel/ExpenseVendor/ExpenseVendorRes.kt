package com.retailone.pos.models.ExpenseRegisterModel.ExpenseVendor

data class ExpenseVendorRes(
    val `data`: List<ExpenseVendorData>,
    val message: String,
    val status: Int
)