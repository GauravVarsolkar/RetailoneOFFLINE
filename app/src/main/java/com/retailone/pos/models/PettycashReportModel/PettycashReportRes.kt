package com.retailone.pos.models.PettycashReportModel

data class PettycashReportRes(
    //val `data`: List<PettyCashData>,
    val `petty_cash_summary`: List<PettyCashData>,
    val pettycash_total_balance: String?,
    val message: String,
    val status: Int
)