package com.retailone.pos.models.CashupModel.CashupSubmit

data class CashupSubmitReq(
    val amount_given_to_bank: Double,
    val cash_payment_actual_amount: Double,
    val cash_payment_entered_amount: Double,
    val cash_refunds: Double,
    val cashup_date_time: String,
    val cit_id: Int,
    val creditcard_actual_payment: Double,
    val creditcard_entered_payment: Double,
    val debitcard_actual_payment: Double,
    val debitcard_entered_payment: Double,
    val expense: Double,
    val mmoney_actual_payment: Double,
    val mmoney_entered_payment: Double,
    val starting_float: Double,
    val closing_balance: Double,
    val store_id: Int,
    val store_manager_id: Int,

    val petty_cash_closing_balance_entered: Double,
    val petty_cash_in: Double,
    val petty_cash_out: Double,
    val pettycash_expected: Double,
   /* val startTotalizer_value: Double,
    val startTotalizer_mode: String,
    val endTotalizer_value: String,
    val endTotalizer_mode: String*/

    )