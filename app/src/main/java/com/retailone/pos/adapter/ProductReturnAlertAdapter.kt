package com.retailone.pos.adapter



import ReturnedProduct
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.databinding.ItemProductBottomsheetBinding
import com.retailone.pos.models.GoodsToWarehouseModel.ReturnStocks.ProductModel

class ProductReturnAlertAdapter(
    private val products: List<ReturnedProduct>
) : RecyclerView.Adapter<ProductReturnAlertAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemProductBottomsheetBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: ReturnedProduct) {
            binding.tvProductName.text = product.product?.product_name ?: "Unknown Product"
            binding.tvQuantity.text = "Qty: ${product.quantity}"
            binding.tvCondition.text = "Condition: ${product.condition}"
            binding.tvApprovedQuantity.text = "Approved quantity: ${product.approved_quantity}"
            binding.tvReceivedQuantity.text = "Received quantity: ${product.received_quantity}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductBottomsheetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount() = products.size
}
