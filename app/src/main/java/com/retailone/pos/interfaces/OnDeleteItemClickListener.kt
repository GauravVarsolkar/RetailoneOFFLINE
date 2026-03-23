package com.retailone.pos.interfaces

import com.retailone.pos.models.CommonModel.StroreProduct.StoreProData

interface OnDeleteItemClickListener {
    fun onDeleteItemClicked(position: StoreProData)
}