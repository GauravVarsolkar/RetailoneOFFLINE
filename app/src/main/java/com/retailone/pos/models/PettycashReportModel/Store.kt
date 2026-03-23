package com.retailone.pos.models.PettycashReportModel

data class Store(
    val address: String,
    val cluster_id: Int,
    val created_at: String,
    val deleted_at: String,
    val ho_manager_id: Int,
    val id: Int,
    val induction_date: String,
    val internal_data: String,
    val latitude: String,
    val location: String,
    val logo: String,
    val logo_image_name: String,
    val longitude: String,
    val organization_id: Int,
    val petty_cash_opening_balance: String?,
    val phone_no: String,
    val station_code: String,
    val status: Int,
    val store_name: String,
    val updated_at: String
)