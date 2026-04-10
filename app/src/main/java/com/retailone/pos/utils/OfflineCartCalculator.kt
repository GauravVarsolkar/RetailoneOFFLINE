package com.retailone.pos.utils

import android.util.Log
import com.retailone.pos.models.CommonModel.StroreProduct.StoreProData
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.AddToCartResData
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.BatchCartItem
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.DistributionPackCart
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.PosAddToCartRes
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.PriceIncTaxItem
import java.math.BigDecimal
import java.math.RoundingMode

object OfflineCartCalculator {

    private const val TAG = "OfflineCartCalc"

    private fun round2(value: Double): Double {
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun calculateCartTotals(cartItems: List<StoreProData>, spotDiscountPercent: Double = 0.0): PosAddToCartRes {

        var grandSubTotal = 0.0
        var grandTax = 0.0
        var grandDiscount = 0.0
        val cartResDataList = mutableListOf<AddToCartResData>()

        var maxTaxRate = 0

        cartItems.forEach { item ->
            val batchCartItems = mutableListOf<BatchCartItem>()
            val priceIncTaxItems = mutableListOf<PriceIncTaxItem>()
            var productSubTotal = 0.0
            var productTax = 0.0
            var productDiscount = 0.0
            var productTaxRate = 0

            item.batch.forEach { batch ->
                val qty = batch.batch_cart_quantity.toInt()
                val priceInclusiveTax = batch.price
                val batchDiscount = batch.discount
                val taxRate = batch.tax?.toIntOrNull() ?: 0

                if (taxRate > productTaxRate) productTaxRate = taxRate
                if (taxRate > maxTaxRate) maxTaxRate = taxRate

                if (qty > 0) {
                    val unitPriceExclTax = if (taxRate == 0) {
                        BigDecimal.valueOf(priceInclusiveTax).setScale(0, RoundingMode.HALF_UP).toDouble()
                    } else {
                        val taxMultiplier = 1.0 + (taxRate / 100.0)
                        BigDecimal.valueOf(priceInclusiveTax / taxMultiplier).setScale(0, RoundingMode.HALF_UP).toDouble()
                    }
                    val itemSubtotal = unitPriceExclTax * qty
                    val taxAmount = ((priceInclusiveTax * qty) - itemSubtotal)

                    productSubTotal += itemSubtotal
                    productTax += taxAmount
                    productDiscount += batchDiscount

                    batchCartItems.add(BatchCartItem(batchno = batch.batch_no, retail_price = priceInclusiveTax, quantity = qty, discount = batchDiscount))
                    priceIncTaxItems.add(PriceIncTaxItem(unit_price = unitPriceExclTax, batch_no = batch.batch_no))
                }
            }

            if (productSubTotal > 0) {
                grandSubTotal += productSubTotal
                grandTax += productTax
                grandDiscount += productDiscount

                val distPack = DistributionPackCart(
                    id = item.distribution_pack_id,
                    product_id = item.product_id,
                    product_description = item.pack_product_description,
                    no_of_packs = item.no_of_packs,
                    uom = item.uom ?: "",
                    barcode = item.barcode ?: "",
                    retail_sku = "",
                    status = 1
                )

                val productTotalWithTax = (productSubTotal + productTax) - productDiscount

                cartResDataList.add(
                    AddToCartResData(
                        distribution_pack_id = item.distribution_pack_id,
                        distribution_pack = distPack,
                        price_without_discount = productSubTotal,
                        product_id = item.product_id,
                        product_name = item.product_name,
                        batch = batchCartItems,
                        stock_id = item.store_id,
                        total = productTotalWithTax,
                        tax_amount = round2(productTax).toString(),
                        tax = productTaxRate,
                        taxrate = productTaxRate.toString(),
                        discount = productDiscount.toInt(),
                        price_inclusive_tax = priceIncTaxItems
                    )
                )
            }
        }

        val subTotalRounded = BigDecimal.valueOf(grandSubTotal).setScale(0, RoundingMode.HALF_UP).toDouble()
        val taxRounded = BigDecimal.valueOf(grandTax).setScale(0, RoundingMode.HALF_UP).toDouble()
        
        // Grand Total before spot discount
        val totalBeforeSpot = (subTotalRounded + taxRounded) - grandDiscount
        
        // Apply spot discount percentage on the total (tax inclusive total matching portal logic)
        val spotDiscountAmount = if (spotDiscountPercent > 0) {
            BigDecimal.valueOf(totalBeforeSpot * (spotDiscountPercent / 100.0))
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()
        } else 0.0

        val finalGrandTotal = totalBeforeSpot - spotDiscountAmount

        return PosAddToCartRes(
            data = cartResDataList,
            discount_amount = grandDiscount,
            grand_total = finalGrandTotal.toString(),
            message = "Offline calculation",
            status = 1,
            sub_total = subTotalRounded,
            sub_total_after_discount = subTotalRounded - grandDiscount,
            tax = "@${maxTaxRate}%",
            tax_amount = taxRounded.toString(),
            spot_discount_percentage = spotDiscountPercent.toString(),
            spot_discount_amount = spotDiscountAmount.toString()
        )
    }
}
