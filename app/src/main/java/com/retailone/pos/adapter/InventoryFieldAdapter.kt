package com.retailone.pos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.databinding.ItemInventoryFieldBinding
import com.retailone.pos.models.ProductInventoryModel.PiResponseModel.InventoryField

class InventoryFieldAdapter(
    private val items: List<InventoryField>
) : RecyclerView.Adapter<InventoryFieldAdapter.FieldViewHolder>() {

    inner class FieldViewHolder(val binding: ItemInventoryFieldBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldViewHolder {
        val binding = ItemInventoryFieldBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FieldViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FieldViewHolder, position: Int) {
        val item = items[position]
        holder.binding.txtLabel.text = item.label
        holder.binding.txtValue.text = item.value
    }

    override fun getItemCount(): Int = items.size
}
