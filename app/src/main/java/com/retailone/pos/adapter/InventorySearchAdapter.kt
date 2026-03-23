package com.retailone.pos.adapter
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.retailone.pos.R
import com.retailone.pos.databinding.StockSearchItemLayoutBinding
import com.retailone.pos.localstorage.SharedPreference.InventoryStockHelper
import com.retailone.pos.models.CommonModel.StroreProduct.StoreProData
import com.retailone.pos.network.Constants

class InventorySearchAdapter(
    private val stockSearchRes: List<StoreProData>, val context: Context
) : RecyclerView.Adapter<InventorySearchAdapter.StockSearchViewHolder> () {

    /* private var matrcvd = MaterialReceived()

     fun setMatRcvdData(matrcvd: MaterialReceived) {
         this.matrcvd = matrcvd
         notifyDataSetChanged()
     }*/


    private val inventoryStockHelper = InventoryStockHelper(context)


    class StockSearchViewHolder(val binding: StockSearchItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockSearchViewHolder {
        return StockSearchViewHolder(
            StockSearchItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: StockSearchViewHolder, position: Int) {

        // val type = matrcvd.materiallist[position].type

        val productitem = stockSearchRes[position]

        holder.binding.itemName.text = productitem.product_name
        holder.binding.itemType.text = productitem.pack_product_description
        //holder.binding.itemPrice.text = productitem.whole_sale_price
        holder.binding.itemPrice.isVisible = false

        holder.binding.addcart.setOnClickListener {
            inventoryStockHelper.saveSearchItem(productitem)
            //initial quantity ad
           // inventoryStockHelper.updateQuantity(productitem.product_id,productitem.distribution_pack_id,"1")

            holder.binding.addlayout.isVisible = false
            // holder.binding.plusMinusLayout.isVisible = true
            //holder.binding.cartProductQuantity.text = "1"
            holder.binding.quantEdit.text = Editable.Factory.getInstance().newEditable("1")
            holder.binding.quantLayout.isVisible = true
            holder.binding.quantEdit.requestFocus()

        }



        cartControl(holder.binding,productitem)


/*
        holder.binding.productimg.setOnClickListener {
            // Handle item click
            // Add the clicked item to the shared preferences list

            // val updatedList = sharedPrefHelper.getSearchResultsList().toMutableList()
            // updatedList.add(productitem)
            // sharedPrefHelper.saveSearchResultsList(updatedList)

            //Toast.makeText(holder.itemView.context,"dfgh",Toast.LENGTH_SHORT).show()

            // You may also perform other actions related to item click

            sharedPrefHelper.saveSearchItem(productitem)
        }
*/




        Glide.with(context)
            .load(Constants.IMAGE_URL + productitem.product_photo)
            .centerCrop() // Add center crop
            .placeholder(R.drawable.temp) // Add a placeholder drawable
            .error(R.drawable.temp) // Add an error drawable (if needed)
            .into(holder.binding.productimg)




    }


    private fun cartControl(binding: StockSearchItemLayoutBinding, productitem: StoreProData) {
/*
        binding.cartPlusImg.setOnClickListener {
            var value = binding.cartProductQuantity.text.toString().toInt()

            if(value < productitem.stock_quantity){
                var newquantity = ++value
                binding.cartProductQuantity.text = (newquantity).toString()
                sharedPrefHelper.updateQuantity(productitem.product_id,productitem.distribution_pack_id,newquantity.toString())
            }else{
                Toast.makeText(context,"Can't Add more item,Limit exceed",Toast.LENGTH_SHORT).show()
            }
        }

        binding.cartMinusImg.setOnClickListener {
            var value = binding.cartProductQuantity.text.toString().toInt()

            if (value>1){
                var newquantity = --value

                binding.cartProductQuantity.text = (newquantity).toString()
                sharedPrefHelper.updateQuantity(productitem.product_id,
                    productitem.distribution_pack_id,(newquantity).toString())
            }else if(value ==1){
                binding.addlayout.isVisible = true
                binding.plusMinusLayout.isVisible = false
                sharedPrefHelper.removeItem(productitem.product_id,productitem.distribution_pack_id)

            }
        }*/

        binding.quantEdit.addTextChangedListener(object:TextWatcher{

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                try {
                    val enteredValue = s.toString().toIntOrNull() ?: 0

                    inventoryStockHelper.updateQuantity(
                        productitem.product_id,
                        productitem.distribution_pack_id,
                        enteredValue
                    )
                } catch (e: NumberFormatException) {
                    // Handle the exception if needed
                }
            }

            override fun afterTextChanged(s: Editable?) {
               // binding.cartProductQuantity.text = s.toString()

            }

        })


    }



    override fun getItemCount(): Int {
        return stockSearchRes.size
    }
}



