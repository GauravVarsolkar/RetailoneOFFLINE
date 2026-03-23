package com.retailone.pos.models.PointofsaleModel.PosAddToCartModel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/*
@Parcelize
data class PosAddToCartRes(
    val data: List<AddToCartResData>,
    val discount_amount: Double,
    val grand_total: String,              // "99,000.00"
    val message: String,
    val status: Int,
    val sub_total: Double,                // 99000
    val sub_total_after_discount: Double, // 99000
    val tax: String,                      // "@0%"
    val tax_amount: String                // "0.00"
) : Parcelable*/
// PosAddToCartRes.kt
@Parcelize
data class PosAddToCartRes(
    val data: List<AddToCartResData>,
    val discount_amount: Double,
    val grand_total: String,
    val message: String,
    val status: Int,
    val sub_total: Double,
    val sub_total_after_discount: Double,
    val tax: String,
    val tax_amount: String
) : Parcelable
