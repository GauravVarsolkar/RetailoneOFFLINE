package com.retailone.pos.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.R
import com.retailone.pos.models.SalesData
import com.retailone.pos.ui.Activity.DashboardActivity.SearchReturnProductActivity

class SalesListAdapter(
    private val context: Context,
    private val salesList: List<SalesData>
) : RecyclerView.Adapter<SalesListAdapter.SalesViewHolder>() {

    inner class SalesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val invoiceId: TextView = itemView.findViewById(R.id.tvInvoiceId)
        val saleDate: TextView = itemView.findViewById(R.id.tvSaleDate)
        val totalAmount: TextView = itemView.findViewById(R.id.tvGrandTotal)
        val paymentType: TextView = itemView.findViewById(R.id.tvPaymentType)
        val returnableFlag: TextView = itemView.findViewById(R.id.tvReturnableFlag)
        val onHoldBadge: TextView? = itemView.findViewById(R.id.tvOnHoldBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sales_list, parent, false)
        return SalesViewHolder(view)
    }

    override fun onBindViewHolder(holder: SalesViewHolder, position: Int) {
        val item = salesList[position]

        holder.invoiceId.text = item.invoice_id
        holder.saleDate.text = item.sale_date_time
        holder.totalAmount.text = "RWF${String.format(java.util.Locale.US, "%.2f", java.math.BigDecimal.valueOf(item.grand_total).setScale(0, java.math.RoundingMode.HALF_UP).toDouble())}"
        holder.paymentType.text = item.payment_type

        // Parse refunded amount
        val refundedAmt = item.total_refunded_amount
            ?.toString()
            ?.toDoubleOrNull() ?: 0.0

        val isOfflineOnHold = com.retailone.pos.localstorage.SharedPreference.OnHoldInvoiceHelper.isOnHold(context, item.invoice_id)
        val isOnHold = (item.on_hold == 1) || isOfflineOnHold

        // Show badge based on return status and on-hold status
        when {
            refundedAmt > 0.0 -> {
                holder.onHoldBadge?.visibility = View.GONE
                holder.returnableFlag.visibility = View.VISIBLE
                holder.returnableFlag.text = "Returned"
                holder.returnableFlag.setBackgroundResource(R.drawable.bg_returned_flag)
            }
            isOnHold -> {
                holder.onHoldBadge?.visibility = View.VISIBLE
                holder.returnableFlag.visibility = View.GONE
            }
            else -> {
                holder.onHoldBadge?.visibility = View.GONE
                holder.returnableFlag.visibility = View.VISIBLE
                holder.returnableFlag.text = "Returnable"
                holder.returnableFlag.setBackgroundResource(R.drawable.bg_returnable_flag)
            }
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, SearchReturnProductActivity::class.java)
            intent.putExtra("invoice_id", item.invoice_id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = salesList.size
}
