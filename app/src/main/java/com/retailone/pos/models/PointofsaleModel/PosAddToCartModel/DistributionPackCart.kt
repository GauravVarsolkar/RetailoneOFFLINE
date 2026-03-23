package com.retailone.pos.models.PointofsaleModel.PosAddToCartModel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/*
@Parcelize
data class DistributionPackCart (
    val id: Int,
    val product_id: Int,
    val product_description: String,
    val no_of_packs: Int,
    val uom: String,
    val barcode: String,
    val retail_sku: String,
    val status: Int

):Parcelable*/
@Parcelize
data class DistributionPackCart (
    val id: Int,
    val product_id: Int,
    val product_description: String,
    val no_of_packs: Int,
    val uom: String,
    val barcode: String,
    val retail_sku: String,
    val status: Int
) : Parcelable