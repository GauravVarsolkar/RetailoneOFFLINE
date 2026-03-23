package com.retailone.pos.interfaces


interface OnReturnQuantityChangeListener {
    fun onReturnQuantityChange(position: Int, newQuantity: Int)
}