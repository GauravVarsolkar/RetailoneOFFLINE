package com.retailone.pos.models.Dispatch

import com.google.gson.annotations.SerializedName

data class DispatchRequest(
    @SerializedName("id")
    val id: Int,
    @SerializedName("seal_no")
    val seal_no: String,
    @SerializedName("vehicle_no")
    val vehicle_no: String,
    @SerializedName("driver_name")
    val driver_name: String
)
