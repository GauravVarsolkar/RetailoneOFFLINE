package com.retailone.pos.models.ProductInventoryModel

import com.retailone.pos.models.PiUpdateModel.PiUpdateItem

data class PiData(
    val status:Boolean = false,
    val pi_parentlist:List<PiParentData> = emptyList()
)
