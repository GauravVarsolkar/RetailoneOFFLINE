package com.retailone.pos.models.GetCustomerModel

data class getCustomerRes(
    val `data`: CustomerData,
    val message: String,
    val status: Int
)