package com.retailone.pos.models.ReturnSalesItemModel

data class StoreDetails(
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
    val store_name: String,
    val updated_at: String
)