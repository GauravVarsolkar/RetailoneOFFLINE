package com.retailone.pos.models.PiUpdateModel


data class PiUpdateData(
    val status:Boolean = false,
    val pi_updatelist:List<PiUpdateItem> = emptyList()
)
