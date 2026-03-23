package com.retailone.pos.models.ProductInventoryModel

import com.retailone.pos.models.PiUpdateModel.PiUpdateItem

data class PiParentData(
    val name:String,
    val desc:String,
    val quantity:String,
    val type:String,
    val pi_childlist:List<PiChildData> = emptyList()

)
