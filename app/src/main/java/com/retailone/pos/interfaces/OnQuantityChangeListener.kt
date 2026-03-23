package com.retailone.pos.interfaces

import com.retailone.pos.models.CommonModel.StroreProduct.PosSaleBatch

interface OnQuantityChangeListener {
    fun onQuantityChange(position: Int, newBatchList: List<PosSaleBatch>)
    //fun onQuantityChange(position: Int, newQuantity: Double)
    //fun onQuantityChange(position: Int, newQuantity: Int)
}