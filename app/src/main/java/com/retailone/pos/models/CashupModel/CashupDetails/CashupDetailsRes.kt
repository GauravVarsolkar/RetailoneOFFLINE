package com.retailone.pos.models.CashupModel.CashupDetails

data class CashupDetailsRes(
    val `data`: CashupDetailsData,
    val message: String,
    val status: Int
)