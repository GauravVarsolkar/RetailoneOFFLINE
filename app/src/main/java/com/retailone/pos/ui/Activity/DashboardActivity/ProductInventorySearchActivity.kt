package com.retailone.pos.ui.Activity.DashboardActivity

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.retailone.pos.R
import com.retailone.pos.adapter.InventorySearchAdapter
import com.retailone.pos.adapter.PointofsaleItemAdapter
import com.retailone.pos.adapter.PosSearchAdapter
import com.retailone.pos.adapter.StockSearchAdapter
import com.retailone.pos.databinding.ActivityStockRequsitionSearchBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.viewmodels.DashboardViewodel.PointofSaleViewmodel
import com.retailone.pos.viewmodels.DashboardViewodel.StockRequsitionSearchViewmodel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProductInventorySearchActivity : AppCompatActivity() {
    lateinit var  binding: ActivityStockRequsitionSearchBinding
    lateinit var stockSearchViewmodel: StockRequsitionSearchViewmodel
    lateinit var inventorySearchAdapter: InventorySearchAdapter
    lateinit var pos_viewmodel: PointofSaleViewmodel
    var storeid = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockRequsitionSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("batch","test7")
        stockSearchViewmodel = ViewModelProvider(this)[StockRequsitionSearchViewmodel::class.java]
        stockSearchViewmodel = ViewModelProvider(this)[StockRequsitionSearchViewmodel::class.java]
        pos_viewmodel = ViewModelProvider(this)[PointofSaleViewmodel::class.java]


        prepareSearchRCV()


        val loginSession= LoginSession.getInstance(this)
        lifecycleScope.launch {
            storeid = loginSession.getStoreID().first().toInt()
            //default search
            pos_viewmodel.callSearchStoreProductApi("",storeid,0,this@ProductInventorySearchActivity)
        }

        pos_viewmodel.loadingLiveData.observe(this){
            binding.progress.isVisible = it.isProgress

            if(it.isMessage)
                showMessage(it.message)
        }

        pos_viewmodel.storeProSearchLivedata.observe(this){


            if(it.data.isNotEmpty()) {


                if (it.data.isNotEmpty()) {
                    inventorySearchAdapter =
                        InventorySearchAdapter(it.data, this@ProductInventorySearchActivity)
                    binding.searchstockRcv.adapter = inventorySearchAdapter
                    binding.searchstockRcv.isVisible = true
                    binding.noDataFound.isVisible = false
                } else {
                    binding.searchstockRcv.isVisible = false
                    binding.noDataFound.isVisible = true
                }

            }
        }
  /*              posSearchAdapter = PosSearchAdapter(it.data,this@PointOfSaleActivity){clickitem->
                    sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

                    //start quantity with 1
                    //clickitem.cart_quantity= 1
                    //posItemList.add(clickitem)
                    updateQuantity(clickitem)

                    //showMessage(clickitem.product_name)

                    positem_adapter= PointofsaleItemAdapter(this,posItemList,this,this  )
                    binding.positemRcv.adapter = positem_adapter


                    // call add to cart api from start
                    calladdToCartAPI()


                }
                incudebinding.searchstockRcv.adapter = posSearchAdapter
                incudebinding.searchstockRcv.isVisible = true
                incudebinding.noDataFound.isVisible = false
            }else{
                incudebinding.searchstockRcv.isVisible = false
                incudebinding.noDataFound.isVisible = true
            }
        }

        stockSearchViewmodel.stockSearchLivedata.observe(this){

            if(it.data.isNotEmpty()){
                inventorySearchAdapter = InventorySearchAdapter(it.data,this@ProductInventorySearchActivity)
                binding.searchstockRcv.adapter = inventorySearchAdapter
                binding.searchstockRcv.isVisible = true
                binding.noDataFound.isVisible = false
            }else{
                binding.searchstockRcv.isVisible = false
                binding.noDataFound.isVisible = true
            }
            *//*    binding.searchstockRcv.isVisible = true
                binding.relativeLayout.isVisible = false*//*
        }*/

        Log.d("batch","test7")
        binding.searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                pos_viewmodel.callSearchStoreProductApi(newText?:"",storeid,0,this@ProductInventorySearchActivity)


                //stockSearchViewmodel.callStockSearchApi(newText?:"",this@ProductInventorySearchActivity)

                return false
            }

        })


        binding.relativeLayout.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra("susess","true")
            // resultIntent.putExtra("id", service.id)
            // resultIntent.putExtra("service_name", service.service_name)
            this.setResult(Activity.RESULT_OK, resultIntent)
            this.finish()
        }

        //default

       /// stockSearchViewmodel.callStockSearchApi("",this@ProductInventorySearchActivity)




    }


    private fun showMessage(msg: String) {
        Toast.makeText(this@ProductInventorySearchActivity, msg, Toast.LENGTH_SHORT).show()
    }



    private fun prepareSearchRCV() {

        binding.searchstockRcv.apply {
            layoutManager = LinearLayoutManager(this@ProductInventorySearchActivity,
                RecyclerView.VERTICAL,false)
        }
    }


}