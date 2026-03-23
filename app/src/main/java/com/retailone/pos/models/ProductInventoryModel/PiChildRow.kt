package com.retailone.pos.models.ProductInventoryModel

import com.retailone.pos.models.ProductInventoryModel.PiResponseModel.DistributionPackData


sealed class PiChildRow {
    data class Header(
        val batchNo: String = "-",
        val stock: Double = 0.0,
        val data: DistributionPackData
    ) : PiChildRow()

    data class Item(
        val status: String,
        val qty: Int,
        val data: DistributionPackData
    ) : PiChildRow()
}
