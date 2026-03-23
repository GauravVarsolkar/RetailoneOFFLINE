package com.retailone.pos.models.CashupModel.CashupSubmit

data class CashupSubmitRes(
    val `data`: CashupSubmitData,
    val message: String,
    val status: Int
)