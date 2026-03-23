package com.retailone.pos.ui.Activity.DashboardActivity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.retailone.pos.R
import com.retailone.pos.adapter.PiCategoryAdapter
import com.retailone.pos.adapter.PiUpdateAdapter
import com.retailone.pos.databinding.ActivityProductInventoryBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.viewmodels.DashboardViewodel.ProductInventoryViewmodel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProductInventoryActivity : AppCompatActivity() {
    lateinit var  binding: ActivityProductInventoryBinding
    lateinit var  piparent_adapter :PiCategoryAdapter
    lateinit var  productInventoryViewmodel: ProductInventoryViewmodel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProductInventoryBinding.inflate(layoutInflater)
        setContentView(binding.root)


        enableBackButton()

        setupPiRCV()

        productInventoryViewmodel = ViewModelProvider(this)[ProductInventoryViewmodel::class.java]


        val loginSession= LoginSession.getInstance(this)
        lifecycleScope.launch {
            val storeid = loginSession.getStoreID().first().toInt()
            productInventoryViewmodel.callProductInventoryApi(storeid,this@ProductInventoryActivity)
        }

        productInventoryViewmodel.inventoryLiveData.observe(this){

            if(it.data.isNotEmpty()){
                piparent_adapter = PiCategoryAdapter(it.data,this)
                binding.piRcv.adapter = piparent_adapter
                binding.noDataFound.isVisible = false
                binding.piRcv.isVisible = true

            }else{
                binding.noDataFound.isVisible = true
                binding.piRcv.isVisible = false
            }

        }

        productInventoryViewmodel.loadingLiveData.observe(this){
            binding.progress.isVisible = it.isProgress

            if(it.isMessage)
                showMessage(it.message)
        }



        /*productInventoryViewmodel.apply {
            //getPiData()
           *//* pi_LiveData.observe(this@ProductInventoryActivity){
                piparent_adapter.setPiData(it)
            }*//*

            callProductInventoryApi()

            piparent_adapter = PiCategoryAdapter()
            adapter = piparent_adapter

        }*/



        binding.relativeLayout.setOnClickListener {
            val intent = Intent(this@ProductInventoryActivity,ProductInventoryUpdateActivity::class.java)
            startActivity(intent)
        }

        setToolbarImage()
    }

    private fun setToolbarImage() {
        val organisation_data = OrganisationDetailsHelper(this).getOrganisationData()

        Glide.with(this)
            .load(organisation_data.image_url + organisation_data.fabicon)
            .fitCenter() // Add center crop
            .placeholder(R.drawable.mlogo) // Add a placeholder drawable
            .error(R.drawable.mlogo) // Add an error drawable (if needed)
            .into(binding.image)
    }

    private fun setupPiRCV() {

        binding.piRcv.apply {
            layoutManager = LinearLayoutManager(this@ProductInventoryActivity,RecyclerView.VERTICAL,false)

        }
    }

    private fun enableBackButton() {
        setSupportActionBar(binding.toolbar)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "New Activity"
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.svg_back_arrow_white)
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this@ProductInventoryActivity, msg, Toast.LENGTH_SHORT).show()
    }

}