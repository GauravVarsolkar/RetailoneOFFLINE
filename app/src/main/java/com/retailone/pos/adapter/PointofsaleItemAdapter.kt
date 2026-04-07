package com.retailone.pos.adapter

import NumberFormatter
import android.content.Context
import android.text.Editable
import android.text.InputType
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.retailone.pos.R
import com.retailone.pos.databinding.PointofsaleItemLayoutBinding
import com.retailone.pos.interfaces.OnDeleteItemClickListener
import com.retailone.pos.interfaces.OnQuantityChangeListener
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.models.CommonModel.StroreProduct.PosSaleBatch
import com.retailone.pos.models.CommonModel.StroreProduct.StoreProData
import com.retailone.pos.network.Constants
import com.retailone.pos.utils.BatchUtils
import android.content.Intent
import com.retailone.pos.utils.FunUtils
import android.util.Log
import com.retailone.pos.ui.Activity.RSSerialActivity
import android.app.Activity
import com.retailone.pos.utils.FeatureManager

class PointofsaleItemAdapter(
    val context: Context,
    private val posItemList: List<StoreProData>,
    private val onDeleteItemClickListener: OnDeleteItemClickListener,
    private val onQuantityChangeListener: OnQuantityChangeListener
) : RecyclerView.Adapter<PointofsaleItemAdapter.POSItemViewHolder>() {


    init {
        // Keep row identity stable (prevents weird redraw issues)
        setHasStableIds(true)
    }

    // One shared pool for all nested RVs -> smoother + less inflation
    companion object {
        private val sharedPool = RecyclerView.RecycledViewPool()
    }

    private val localizationData = LocalizationHelper(context).getLocalizationData()

    class POSItemViewHolder(val binding: PointofsaleItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): POSItemViewHolder {
        return POSItemViewHolder(
            PointofsaleItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: POSItemViewHolder, position: Int) {
        val item = posItemList[position]

        val isLooseoil = FunUtils.isLooseOil(item.category_id, item.pack_product_description)
        holder.binding.quantEdit.inputType =
            if (isLooseoil)
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            else
                InputType.TYPE_CLASS_NUMBER

        holder.binding.itemName.text = item.product_name
        holder.binding.quantEdit.text =
            Editable.Factory.getInstance().newEditable(FunUtils.DtoString(item.cart_quantity))
        holder.binding.itemDesc.text = item.pack_product_description.toString()

        val formattedPrice =
            NumberFormatter().formatPrice(item.retail_price ?: "-", localizationData)
        holder.binding.itemPrice.text = formattedPrice

        holder.binding.itemUnit.text =
            "${FunUtils.DtoString(BatchUtils.getTotalPosQuantity(item.batch))} ${if (isLooseoil) "Liters" else "Units"}"
        holder.binding.itemUnit.setTextColor(Color.parseColor("#008000"))

        // ----- Child RecyclerView set up -----
        holder.binding.batchRcv.apply {
            // Let child measure fully; nested scroll off avoids height conflicts
            isNestedScrollingEnabled = false
            visibility = View.VISIBLE

            if (layoutManager == null) {
                layoutManager = LinearLayoutManager(holder.itemView.context, RecyclerView.VERTICAL, false)
            }
            setRecycledViewPool(sharedPool)
            setHasFixedSize(true)
            itemAnimator = null // reduces rebind flicker

            adapter = PointofsaleBatchAdapter(
                context = context,
                posItemList = posItemList,
                onDeleteItemClickListener = onDeleteItemClickListener,
                onQuantityChangeListener = onQuantityChangeListener,
                batchList = item.batch,
                parentposition = holder.adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: position
            ) { newBatches: List<PosSaleBatch> ->
                // Use fresh adapter position to be safe
                val livePos = holder.adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: position
                onQuantityChangeListener.onQuantityChange(livePos, newBatches)
            }
        }

        holder.binding.delete1.setOnClickListener {
            val livePos = holder.adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: position
            onDeleteItemClickListener.onDeleteItemClicked(posItemList[livePos])
        }

        Glide.with(context)
            .load(Constants.IMAGE_URL + item.product_photo)
            .centerCrop()
            .placeholder(R.drawable.temp)
            .error(R.drawable.temp)
            .into(holder.binding.productimg)

        // Dispense UI (unchanged logic)
        // Dispense UI (respecting FeatureManager)
        val isTotalizerEnabled = FeatureManager.isEnabled("totalizer")

        if (isTotalizerEnabled && isLooseoil) {
            holder.binding.dispancelayout.setOnClickListener {
                var batch_quantity_input = 0.0
                var price = 0.0

                if (item.batch.isNotEmpty()) {
                    batch_quantity_input = item.batch[0].batch_cart_quantity
                    price = item.batch[0].price
                }

                val intent = Intent(context, RSSerialActivity::class.java)
                Log.d("MyApp", "price:$price")
                intent.putExtra("price", price)
                intent.putExtra("pro_id", item.product_id)
                intent.putExtra("dis_id", item.distribution_pack_id)
                val activity = context as Activity
                activity.startActivityForResult(intent, 200)
            }

            if (item.dispense_status == 2) {
                holder.binding.dispancelayout.visibility = View.VISIBLE
                holder.binding.dispancelayout.isEnabled = false
                holder.binding.distext.visibility = View.GONE
                holder.binding.discomplete.visibility = View.VISIBLE
                holder.binding.quantEdit.isEnabled = false
                holder.binding.delete1.visibility = View.GONE
            } else if (item.dispense_status == 1) {
                holder.binding.dispancelayout.visibility = View.VISIBLE
                holder.binding.dispancelayout.isEnabled = true
                holder.binding.distext.visibility = View.VISIBLE
                holder.binding.discomplete.visibility = View.GONE
                holder.binding.quantEdit.isEnabled = true
                holder.binding.delete1.isEnabled = true
            } else {
                holder.binding.dispancelayout.visibility = View.GONE
            }
        } else {
            holder.binding.dispancelayout.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = posItemList.size

    // One view type is sufficient
    override fun getItemViewType(position: Int) = 0

    // Stable id from (productId, packId)
    override fun getItemId(position: Int): Long {
        val pid = posItemList[position].product_id.toLong()
        val did = posItemList[position].distribution_pack_id.toLong()
        return (pid shl 32) xor (did and 0xffffffffL)
    }
}
