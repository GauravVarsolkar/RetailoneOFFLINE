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

    fun calculateCartTotals(cartItems: List<StoreProData>): PosAddToCartRes {

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

            // Track per-product tax rate (use first batch's tax as representative for the product)
            var productTaxRate = 0

            Log.d(TAG, "========================================")
            Log.d(TAG, "Product: ${item.product_name}")

            item.batch.forEach { batch ->
                val qty = batch.batch_cart_quantity.toInt()
                val priceInclusiveTax = batch.price
                val batchDiscount = batch.discount

                // ✅ Get tax rate from each batch (from API), default to 0 if null/empty
                val taxRate = batch.tax?.toIntOrNull() ?: 0

                // Track the max tax rate for this product
                if (taxRate > productTaxRate) {
                    productTaxRate = taxRate
                }

                if (taxRate > maxTaxRate) {
                    maxTaxRate = taxRate
                }

                Log.d(TAG, "  Batch ${batch.batch_no}: Tax Rate = $taxRate%")

                if (qty > 0) {

                    // ✅ STEP 1: Calculate unit price (tax exclusive)
                    // We round to 0 decimals to match the online screenshot (RF 8,475.00)
                    val unitPriceExclTax = if (taxRate == 0) {
                        BigDecimal.valueOf(priceInclusiveTax).setScale(0, RoundingMode.HALF_UP).toDouble()
                    } else {
                        val taxMultiplier = 1.0 + (taxRate / 100.0)
                        BigDecimal.valueOf(priceInclusiveTax / taxMultiplier).setScale(0, RoundingMode.HALF_UP).toDouble()
                    }

                    // ✅ STEP 2: Subtotal = unit_price × qty
                    val itemSubtotal = unitPriceExclTax * qty

                    // ✅ STEP 3: Tax based on ORIGINAL price (Before Discount)
                    // Difference between inclusive price and exclusive subtotal
                    // Matches online mode (RF 10,000 - RF 8,475 = RF 1,525)
                    val taxAmount = ((priceInclusiveTax * qty) - itemSubtotal)

                    // ✅ STEP 4: Accumulate totals
                    productSubTotal += itemSubtotal
                    productTax += taxAmount
                    productDiscount += batchDiscount

                    Log.d(TAG, "  Batch ${batch.batch_no}:")
                    Log.d(TAG, "    Qty: $qty")
                    Log.d(TAG, "    Price (inc tax): $priceInclusiveTax")
                    Log.d(TAG, "    Unit Price (ex tax): $unitPriceExclTax")
                    Log.d(TAG, "    Subtotal: $itemSubtotal")
                    Log.d(TAG, "    Tax: $taxAmount")
                    Log.d(TAG, "    Discount: $batchDiscount")

                    batchCartItems.add(
                        BatchCartItem(
                            batchno = batch.batch_no,
                            retail_price = priceInclusiveTax,
                            quantity = qty,
                            discount = batchDiscount
                        )
                    )

                    priceIncTaxItems.add(
                        PriceIncTaxItem(
                            unit_price = unitPriceExclTax,
                            batch_no = batch.batch_no
                        )
                    )
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

                // ✅ Product Total = (SubTotal + Tax) - Discount
                // For a 10k product with 1k discount, this results in exactly (8475 + 1525) - 1000 = 9000
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
                        tax_amount = productTax.toInt().toString(),
                        tax = productTaxRate,
                        taxrate = productTaxRate.toString(),
                        discount = productDiscount.toInt(),
                        price_inclusive_tax = priceIncTaxItems
                    )
                )

                Log.d(TAG, "Product Summary:")
                Log.d(TAG, "  Tax Rate: $productTaxRate%")
                Log.d(TAG, "  Subtotal (after discount): $productSubTotal")
                Log.d(TAG, "  Tax: $productTax")
                Log.d(TAG, "  Total with tax: $productTotalWithTax")
            }
        }

        // ✅ Final totals (already calculated correctly, no additional rounding needed)
        val subTotalRounded = BigDecimal.valueOf(grandSubTotal)
            .setScale(0, RoundingMode.HALF_UP)
            .toDouble()

        val taxRounded = BigDecimal.valueOf(grandTax)
            .setScale(0, RoundingMode.HALF_UP)
            .toDouble()

        val grandTotal = (subTotalRounded + taxRounded) - grandDiscount

        Log.d(TAG, "========================================")
        Log.d(TAG, "FINAL TOTALS:")
        Log.d(TAG, "  Subtotal: $subTotalRounded")
        Log.d(TAG, "  Tax: $taxRounded")
        Log.d(TAG, "  Discount: $grandDiscount")
        Log.d(TAG, "  Grand Total: $grandTotal")
        Log.d(TAG, "  Max Tax Rate: $maxTaxRate%")
        Log.d(TAG, "========================================")

        return PosAddToCartRes(
            data = cartResDataList,
            discount_amount = grandDiscount,
            grand_total = grandTotal.toString(),
            message = "Offline calculation",
            status = 1,
            sub_total = subTotalRounded,
            sub_total_after_discount = subTotalRounded - grandDiscount,
            tax = "@${maxTaxRate}%",
            tax_amount = taxRounded.toString()
        )
    }
}
