package com.retailone.pos.models.PointofsaleModel

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class BatchCartItems(
    @SerializedName("batchno")
    val batchno: String,             // keep your existing property name so other code compiles

    @SerializedName("retail_price")
    val retail_price: Double,

    // quantity in the JSON is a number (0). Keep it numeric to avoid parse errors.
    @SerializedName("quantity")
    val quantity: Int
) : Parcelable