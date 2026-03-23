package com.retailone.pos.models.MaterialRcvModel

data class MaterialReceived(
    val status:Boolean = false,
    val materiallist:List<MaterialRcvItemtest> = emptyList()
)
