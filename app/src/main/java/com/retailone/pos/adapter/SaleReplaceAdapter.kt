package com.retailone.pos.adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.R
import com.retailone.pos.models.SalesData
import com.retailone.pos.ui.Activity.DashboardActivity.SearchReplaceProductActivity

class SaleReplaceAdapter(
    private val context: Context,
    private val salesList: List<SalesData>
) : RecyclerView.Adapter<SaleReplaceAdapter.SalesViewHolder>() {

    companion object {
        private const val TAG = "SaleReplaceAdapter"
    }

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
            .inflate(R.layout.item_sale_replaced, parent, false)
        return SalesViewHolder(view)
    }

    override fun onBindViewHolder(holder: SalesViewHolder, position: Int) {
        val item = salesList[position]

        holder.invoiceId.text = item.invoice_id
        holder.saleDate.text = item.sale_date_time
        holder.totalAmount.text = "RWF${item.grand_total}"
        holder.paymentType.text = item.payment_type

        // Parse amounts
        val refundedAmt = item.total_refunded_amount
            ?.toString()
            ?.toDoubleOrNull() ?: 0.0

        val replacedAmt = item.total_replaced_amount
            ?.toString()
            ?.toDoubleOrNull() ?: 0.0

        // Get on_hold value from backend
        val isOnHold = (item.on_hold == 1)

        // Badge Display Logic (WITH ON HOLD)
        when {
            // Priority 1: Already Returned
            refundedAmt > 0.0 -> {
                holder.onHoldBadge?.isVisible = false
                holder.returnableFlag.isVisible = true
                holder.returnableFlag.text = "Returned"
                holder.returnableFlag.setBackgroundResource(R.drawable.bg_returned_flag)
            }

            // Priority 2: On Hold (from backend)
            isOnHold -> {
                holder.onHoldBadge?.isVisible = true
                holder.returnableFlag.isVisible = false
            }

            // Priority 3: Replaceable (not yet replaced)
            replacedAmt == 0.0 -> {
                holder.onHoldBadge?.isVisible = false
                holder.returnableFlag.isVisible = true
                holder.returnableFlag.text = "Replaceable"
                holder.returnableFlag.setBackgroundResource(R.drawable.bg_returnable_flag)
            }

            // Priority 4: Already Replaced (no badge)
            else -> {
                holder.onHoldBadge?.isVisible = false
                holder.returnableFlag.isVisible = true
                holder.returnableFlag.text = "Replaced"
                holder.returnableFlag.setBackgroundResource(R.drawable.bg_returned_flag)
            }
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, SearchReplaceProductActivity::class.java)
            intent.putExtra("invoice_id", item.invoice_id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = salesList.size
}





/*
class SaleReplaceAdapter(private val context: Context, private val salesList: List<SalesData>) :
    RecyclerView.Adapter<SaleReplaceAdapter.SalesViewHolder>() {

    inner class SalesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val invoiceId = itemView.findViewById<TextView>(R.id.tvInvoiceId)
        val saleDate = itemView.findViewById<TextView>(R.id.tvSaleDate)
        val totalAmount = itemView.findViewById<TextView>(R.id.tvGrandTotal)
        val paymentType = itemView.findViewById<TextView>(R.id.tvPaymentType)
        val returnableFlag = itemView.findViewById<TextView>(R.id.tvReturnableFlag)

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sale_replaced, parent, false)
        return SalesViewHolder(view)
    }

    override fun onBindViewHolder(holder: SalesViewHolder, position: Int) {
        val item = salesList[position]
        holder.invoiceId.text = item.invoice_id
        holder.saleDate.text = item.sale_date_time
        holder.totalAmount.text = "RWF${item.grand_total}"
        holder.paymentType.text = item.payment_type
        // 🔥 Click to launch activity with invoice_id
        // 🔺 Show flag if total_refunded_amount == 0.0
        if (item.total_replaced_amount == 0.0) {
            holder.returnableFlag.visibility = View.VISIBLE
        } else {
            holder.returnableFlag.visibility = View.GONE
        }
        holder.itemView.setOnClickListener {
            val intent = Intent(context, SearchReplaceProductActivity::class.java)
            intent.putExtra("invoice_id", item.invoice_id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = salesList.size
}
*/
