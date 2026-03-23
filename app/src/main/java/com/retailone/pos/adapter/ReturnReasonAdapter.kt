package com.retailone.pos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.retailone.pos.databinding.CustomDropdownItemLayoutBinding
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseCategory.ExpenseCategoryData
import com.retailone.pos.models.ReturnSalesItemModel.SalesReturnReasonModel.ReturnReasonData

class ReturnReasonAdapter(
    context: Context,
    resource: Int,
    objects: List<ReturnReasonData>
) : ArrayAdapter<ReturnReasonData>(context, resource, objects) {



    /* private  var distlist = ArrayList<Data>()

     fun setDistList(distlist :List<Data>){

         this.distlist = distlist as ArrayList<Data>
         notifyDataSetChanged()
     }*/

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    private fun initView(position: Int, convertView: View?, parent: ViewGroup): View {

        lateinit var binding: CustomDropdownItemLayoutBinding

        var listitemview = convertView
        /*
                if(listitemview == null){
                    binding = CustomDropdownItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
                    listitemview = binding.root
                }*/

        binding = CustomDropdownItemLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        listitemview = binding.root

        val currentItem: ReturnReasonData? = getItem(position)

        binding.adapterText.text = currentItem?.reason_name

        return listitemview

    }



}