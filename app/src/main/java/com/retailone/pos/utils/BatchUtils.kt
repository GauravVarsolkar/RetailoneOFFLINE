package com.retailone.pos.utils

import android.util.Log
import com.retailone.pos.models.CommonModel.StroreProduct.PosSaleBatch

object BatchUtils {

    fun getTotalPosQuantity(batchList: List<PosSaleBatch>): Double {
        return batchList.sumOf { it.quantity ?: 0.0 } // If quantity is null, use 0.0
    }

    fun getTotalPosPrice(batchList: List<PosSaleBatch>): Double {
        return batchList.sumOf { it.price ?: 0.0 } // If quantity is null, use 0.0
    }

    fun getTotalPosCartQuantity(batchList: List<PosSaleBatch>): Double {
        return batchList.sumOf { it.batch_cart_quantity ?: 0.0 } // If quantity is null, use 0.0
    }
}