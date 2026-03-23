package com.retailone.pos.models.LocalizationModel

data class LocalizationData(
    val currency: String,
    val date_time_format: String,
    val thousand_separator: String,
    val timezone: String
)