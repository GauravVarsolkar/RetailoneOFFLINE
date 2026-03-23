package com.retailone.pos.models.PointofsaleModel.PosAddToCartModel

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.retailone.pos.models.PointofsaleModel.BatchCartItems
import kotlinx.parcelize.Parcelize

/*@Parcelize
data class AddToCartResData(
    val distribution_pack_id: Int,
    val distribution_pack: DistributionPackCart,
    val price_without_discount: Double,   // <-- was String
    val product_id: Int,
    val product_name: String,
    val batch: List<BatchCartItem>,    // see #2: use a response-safe class
    val stock_id: Int,
    val total: Double,

    // Optional (present in JSON; keep nullable so parse never fails if missing):
    val tax_amount: String? = null,
    val price_inclusive_tax: List<PriceIncTaxItem>? = null
) : Parcelable

@Parcelize
data class PriceIncTaxItem(
    val unit_price: Double,
    val batch_no: String
) : Parcelable*/
// AddToCartResData.kt
@Parcelize
data class AddToCartResData(
    val distribution_pack_id: Int,
    val distribution_pack: DistributionPackCart,
    val price_without_discount: Double,
    val product_id: Int,
    val product_name: String,
    val batch: List<BatchCartItem>,
    val stock_id: Int,
    val total: Double,
    val tax_amount: String? = null,
    val tax: Int,
    val discount: Int,
    val taxrate: String,
    val price_inclusive_tax: List<PriceIncTaxItem>? = null
) : Parcelable

@Parcelize
data class PriceIncTaxItem(
    val unit_price: Double,
    @SerializedName("batch_no") val batch_no: String
) : Parcelable
