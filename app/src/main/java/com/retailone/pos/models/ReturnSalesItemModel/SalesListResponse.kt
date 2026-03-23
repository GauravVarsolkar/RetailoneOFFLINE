package com.retailone.pos.models

import com.google.gson.annotations.SerializedName

data class SalesListResponse(
    val status: Int,
    val message: String?,
    val data: List<SalesData>
)

data class SalesData(
    val id: Int,
    val store_id: Int,
    val store_manager_id: Int,
    val payment_type: String,
    val sub_total: Double,
    val tax: String,
    val tax_amount: Double,
    val discount_amount: Double,
    val subtotal_after_discount: Double,
    val grand_total: Double,
    val amount_tendered: Double,
    val sale_date_time: String,
    val invoice_id: String,
    val total_refunded_amount: Double,
    val total_replaced_amount: Double,
    val created_at: String?,
    val updated_at: String?,
    val status: Int,
    val canceled_at: String?,
    val reversal_of: String?,
    val store_details: StoreDetails?,
    val store_manager_details: StoreManagerDetails?,
    val sales_items: List<SalesItem>,
    val customer: Customer?,
    val on_hold: Int? = null
)

data class StoreDetails(
    val id: Int,
    val store_name: String,
    val station_code: String,
    val address: String,
    val organization_id: Int,
    val ho_manager_id: Int,
    val cluster_id: Int,
    val phone_no: String,
    val logo: String,
    val logo_image_name: String,
    val latitude: String,
    val longitude: String,
    val induction_date: String,
    val location: String,
    val internal_data: String,
    val petty_cash_opening_balance: String?,
    val deleted_at: String?,
    val created_at: String?,
    val updated_at: String?,
    val status: Int,
    val exposure_limit: Int
)

data class StoreManagerDetails(
    val id: Int,
    val user_type: String?,
    val surname: String?,
    val first_name: String,
    val last_name: String?,
    val username: String?,
    val email: String,
    val email_verified_at: String?,
    val password: String,
    val password_changed_at: String?,
    val role_id: String,
    val cluster_id: String?,
    val contact_no: String,
    val alt_contact_no: String?,
    val address: String?,
    val permanent_address: String?,
    val current_address: String?,
    val business_id: String,
    val allow_login: Int,
    val active_status: String,
    val dob: String,
    val gender: String?,
    val created_at: String?,
    val updated_at: String?,
    val deleted_at: String?,
    val status: Int
)

data class SalesItem(
    val id: Int,
    val sales_id: String,
    val product_id: Int,
    val product_name: String,
    val distribution_pack_id: Int,
    val distribution_pack_name: String,
    val quantity: Int,
    val whole_sale_price: Double,
    val retail_price: Double,
    val total_amount: Double,
    val batch: String,
    val created_at: String,
    val updated_at: String,
    val status: Int,
    val sales_return_id: String?,
    val on_hold: Int? = null
)

data class Customer(
    val id: Int,
    val sales_id: String,
    val customer_name: String?,
    val customer_mob_no: String?,
    val created_at: String,
    val updated_at: String,
    val status: Int
)
