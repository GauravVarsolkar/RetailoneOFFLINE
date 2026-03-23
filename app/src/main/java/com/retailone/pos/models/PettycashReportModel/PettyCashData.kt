package com.retailone.pos.models.PettycashReportModel

data class PettyCashData(
    val id: Int,
    val store_id: Int,
    val txn_date: String,
    //val doc_attachment: String,
    val pettycash_opening_balance: Double?,
    val total_txn_amount: Double?,
    val total_expenses: Double?,
    val pettycash_closing_balance_actual: Double?,

)