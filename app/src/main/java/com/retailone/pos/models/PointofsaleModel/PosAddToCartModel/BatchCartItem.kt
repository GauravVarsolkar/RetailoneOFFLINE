package com.retailone.pos.models.PointofsaleModel.PosAddToCartModel

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/*
@Parcelize
data class BatchCartItem(
    val batchno: String,
    val retail_price: Double,
    val quantity: String,
  //  val reorder_level: Int
) : Parcelable*/
@Parcelize
data class BatchCartItem(
    @SerializedName("batch_no") val batchno: String,             // keep your existing property name so other code compiles

    @SerializedName("retail_price") val retail_price: Double,

    // quantity in the JSON is a number (0). Keep it numeric to avoid parse errors.
    @SerializedName("quantity") val quantity: Int,

    @SerializedName("discount") val discount: Double


) : Parcelable