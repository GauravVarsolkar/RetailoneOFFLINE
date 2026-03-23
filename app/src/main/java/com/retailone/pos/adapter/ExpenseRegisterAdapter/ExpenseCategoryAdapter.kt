package com.retailone.pos.adapter.ExpenseRegisterAdapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.retailone.pos.databinding.CustomDropdownItemLayoutBinding
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseCategory.ExpenseCategoryData

class ExpenseCategoryAdapter(
    context: Context,
    resource: Int,
    objects: List<ExpenseCategoryData>
) : ArrayAdapter<ExpenseCategoryData>(context, resource, objects) {



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

        val currentItem: ExpenseCategoryData? = getItem(position)

        binding.adapterText.text = currentItem?.category_name

        return listitemview

    }



}