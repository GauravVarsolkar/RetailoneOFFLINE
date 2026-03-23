package com.retailone.pos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.databinding.ProductReorderAlertLayoutBinding
import com.retailone.pos.models.MaterialRcvModel.MaterialReceived

class ProductReorderAlertAdapter  : RecyclerView.Adapter<ProductReorderAlertAdapter.ProductReorderViewHolder> (){

    private var matrcvd = MaterialReceived()

    fun setMatRcvdData(matrcvd : MaterialReceived){
        this.matrcvd = matrcvd
        notifyDataSetChanged()
    }

    class ProductReorderViewHolder (val binding:ProductReorderAlertLayoutBinding) : RecyclerView.ViewHolder(binding.root){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductReorderViewHolder {
        return ProductReorderViewHolder(ProductReorderAlertLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: ProductReorderViewHolder, position: Int) {

        holder.binding.itemName.text = matrcvd.materiallist[position].name

    }


    override fun getItemCount(): Int {
        return matrcvd.materiallist.size
    }


}

