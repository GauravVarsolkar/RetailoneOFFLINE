package com.retailone.pos.models.UserProfileModels

data class UserDetails(
    val active_status: String,
    val address: Any,
    val allow_login: String,
    val alt_contact_no: String,
    val business_id: String,
    val contact_no: String,
    val created_at: String,
    val current_address: String,
    val deleted_at: Any,
    val dob: String,
    val email: String,
    val email_verified_at: Any,
    val first_name: String,
    val gender: String,
    val id: Int,
    val last_name: String,
    val permanent_address: String,
    val role_id: String,
    val status: String,
    val store_id: String,
    val surname: Any,
    val updated_at: String,
    val user_type: Any,
    val username: Any,
    val store_name: String?,
    val organization: OrganizationDetails? = null  // ✅ NEW
)

data class OrganizationDetails(
    val id: Int,
    val name: String,
    val modules: List<String> = emptyList()
)