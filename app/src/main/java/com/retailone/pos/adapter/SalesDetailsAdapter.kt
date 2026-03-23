package com.retailone.pos.adapter

import NumberFormatter
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.databinding.SalesDetailsItemLayoutBinding
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.SalesDetailsData
import com.retailone.pos.utils.FunUtils
import java.math.BigDecimal
import java.math.RoundingMode

class SalesDetailsAdapter(
    val context: Context,
    val salesdetails: SalesDetailsData
) : RecyclerView.Adapter<SalesDetailsAdapter.SalesDetailsViewHolder>() {

    private val localizationData = LocalizationHelper(context).getLocalizationData()

    class SalesDetailsViewHolder(val binding: SalesDetailsItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalesDetailsViewHolder {
        return SalesDetailsViewHolder(
            SalesDetailsItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: SalesDetailsViewHolder, position: Int) {

        val item = salesdetails.sales_items[position]

        // Format total amount
        val formattedPrice = NumberFormatter().formatPrice(
            item.total_amount.toString(),
            localizationData
        )

        // Format the tax variable (Int)
        val taxValue = item.tax ?: 0
        val formattedTax = NumberFormatter().formatPrice(
            taxValue.toString(),
            localizationData
        )

        // Format tax amount
        val rawTax = item.tax_amount ?: 0.0
        val roundedTaxStr = BigDecimal.valueOf(rawTax)
            .setScale(0, RoundingMode.HALF_UP)
            .toPlainString()

        val formattedTaxAmount = NumberFormatter().formatPrice(
            roundedTaxStr,
            localizationData
        )

        // Format subtotal
        val roundedSubTotalStr = BigDecimal(item.sub_total.toString())
            .setScale(0, RoundingMode.HALF_UP)
            .toPlainString()

        val formattedSubTotal = NumberFormatter().formatPrice(
            roundedSubTotalStr,
            localizationData
        )

        // ✅ Get discount value from backend
        val discountValue = item.discount ?: 0

        // ✅ Format discount amount
        val roundedDiscount = BigDecimal.valueOf(discountValue.toDouble())
            .setScale(0, RoundingMode.HALF_UP)
            .toPlainString()

        val formattedDiscount = NumberFormatter().formatPrice(
            roundedDiscount,
            localizationData
        )

        holder.binding.apply {
            date.text = item.product.product_name
            name.text = item.distribution_pack_name
            category.text = item.total_quantity.let { FunUtils.DtoString(it) }

            tax.text = "(+) Tax @${item.tax ?: 0}%:   "
            taxamount.text = formattedTaxAmount
            subtotal.text = formattedSubTotal
            totalamount.text = formattedPrice

            // ✅ Show discount row if discount exists
            if (discountValue > 0) {
                discountRow.isVisible = true
                discountamount.text = formattedDiscount
            } else {
                discountRow.isVisible = false
            }
        }
    }


    override fun getItemCount(): Int {
        return salesdetails.sales_items.size
    }
}