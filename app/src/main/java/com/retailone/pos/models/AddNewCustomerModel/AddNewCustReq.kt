package com.retailone.pos.models.AddNewCustomerModel

data class AddNewCustReq(
    val customer_name: String,
    val mobile_no: String,
    val tin_tpin_no: String
)