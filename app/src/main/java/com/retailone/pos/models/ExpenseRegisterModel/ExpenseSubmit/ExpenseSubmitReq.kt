package com.retailone.pos.models.ExpenseRegisterModel.ExpenseSubmit

data class ExpenseSubmitReq(
    val amount: String,
    val category_name: String,
    val invoice: String,
    val store_manager_id: String,
    val expense_date_time: String,
    val store_id: String,
    val vendor_name: String,

    val vat: String,
    val total_amount: String,
    val sdc_no: String,
    val receipt_no: String,
    val remarks: String,
)
