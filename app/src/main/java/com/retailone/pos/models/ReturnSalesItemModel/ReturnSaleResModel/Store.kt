package com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel

/*
data class Store(
    val address: String,
    val cluster_id: Int,
    val created_at: String,
    val deleted_at: String,
    val ho_manager_id: Int,
    val id: Int,
    val induction_date: String,
    val latitude: String,
    val location: String,
    val logo: String,
    val longitude: String,
    val organization_id: Int,
    val phone_no: String,
    val station_code: String,
    val status: Int,
    val store_incharge: StoreIncharge,
    val store_name: String,
    val updated_at: String
)*/
// Store.kt


data class Store(
    val id: Int,
    val store_name: String,
    val station_code: String,
    val address: String,
    val organization_id: Int,
    val ho_manager_id: Int,
    val cluster_id: Int,
    val phone_no: String,
    val logo: String?,                 // may include CRLF in payload
    val logo_image_name: String? = null,
    val latitude: String,
    val longitude: String,
    val induction_date: String,
    val location: String,
    val internal_data: String?,
    val petty_cash_opening_balance: String? = null,
    val deleted_at: String?,           // e.g. "2025-09-09 16:38:54"
    val created_at: String,
    val updated_at: String,
    val status: Int,
    val exposure_limit: String? = null,

    val store_incharge: StoreIncharge?
)

