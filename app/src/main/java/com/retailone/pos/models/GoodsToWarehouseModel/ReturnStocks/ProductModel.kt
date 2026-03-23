package com.retailone.pos.models.GoodsToWarehouseModel.ReturnStocks

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class ProductModel(
    val id: Int,
    val product_name: String,
    val product_description: String,
    val type: String,
    val category_id: Int,
    val tax_id: Int,
    val photo: String?,
    val photo_name: String?,
    val deleted_at: String?,
    val created_at: String?,
    val updated_at: String?,
    val status: Int
) : Parcelable