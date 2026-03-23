package com.retailone.pos.models.OrganisationDetailsModel

data class OrganisationDetailsRes(
    val `data`: OrganisationData,
    val message: String,
    val status: Int
)