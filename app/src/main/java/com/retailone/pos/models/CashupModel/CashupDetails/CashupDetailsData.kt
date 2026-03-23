package com.retailone.pos.models.CashupModel.CashupDetails

data class CashupDetailsData(
    val amount_given_to_bank: Double,
    val cash_payment_actual_amount: Double,
    val cash_payments: Double,
    val cash_payment_entered_amount: Double,
    val cash_refunds: Double,
    val cit_id: Int?,
    val creditcard_actual_payment: Double,
    val creditcard_entered_payment: Double,
    val debitcard_actual_payment: Double,
    val debitcard_entered_payment: Double,
    val expenses: Double,
    val mmoney_actual_payment: Double,
    val mmoney_entered_payment: Double,
    val starting_float: Double,
    val store_id: Int,
    val store_manager_id: String,
    val cashup_type: CashupType?,


    val petty_cash_in: Double?,
    val petty_cash_out: Double?,
    val pettycash_expected: Double?,
    val PettyCashOpeningbalance: Double?,

    )
data class CashupType(
    val name: String
)