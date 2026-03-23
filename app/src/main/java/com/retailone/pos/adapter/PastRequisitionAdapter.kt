package com.retailone.pos.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.databinding.PastRequisitionItemLayoutBinding
import com.retailone.pos.models.StockRequisitionModel.PastRequsitionModel.PastRequisitionData
import com.retailone.pos.utils.DateFormatter

class PastRequisitionAdapter(
    val context: Context,
    val pastRequsitionRes: List<PastRequisitionData>,
    val type: String,
    val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<PastRequisitionAdapter.PastRequisitionViewHolder>() {


    class PastRequisitionViewHolder(val binding: PastRequisitionItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PastRequisitionViewHolder {
        return PastRequisitionViewHolder(
            PastRequisitionItemLayoutBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: PastRequisitionViewHolder, position: Int) {

        val item = pastRequsitionRes[position]

        val datemodel = DateFormatter(item.pi_date).formatDateModel()


        holder.binding.apply {
            itemName.text = "ID - ${item.order_id}"
            date.text = datemodel.day.toString()
            month.text = datemodel.month.toString()
            itemQuantity.text = "${item.order_items.size} items"

            setStatusString(item.status, status)
        }


        holder.binding.card.setOnClickListener {
            onItemClick(item.id.toString())  // request id
        }

    }

    private fun setStatusString(status: String, textview: TextView) {

        when (status) {
            "0" -> {
                textview.text = "Pending"
                // textview.setTextColor(Color.parseColor("#FF681E"))
                textview.setTextColor(Color.parseColor("#FF980E"))
            }

            "1" -> {
                textview.text = "Approved"
                textview.setTextColor(Color.parseColor("#0496c7"))
            }

            "2" -> {
                textview.text = "Cancelled"
                textview.setTextColor(Color.parseColor("#D3212C"))
            }
            "4" -> {
                textview.text = "Dispatched"
                textview.setTextColor(Color.parseColor("#673AB7"))
            }

            "3" -> {
                textview.text = "Received"
                textview.setTextColor(Color.parseColor("#008000"))
            }
            "5" -> {
                textview.text = "Rejected"
                textview.setTextColor(Color.parseColor("#D3212C"))
            }

            else -> {
                textview.text = "."
            }
        }
    }


    override fun getItemCount(): Int {
        var size = pastRequsitionRes.size

        if (type != "all" && size > 3) {
            size = 3
        }

        return size
    }

}