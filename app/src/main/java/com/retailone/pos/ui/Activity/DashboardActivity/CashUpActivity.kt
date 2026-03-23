package com.retailone.pos.ui.Activity.DashboardActivity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.bumptech.glide.Glide
import com.retailone.pos.R
import com.retailone.pos.databinding.ActivityCashUpBinding
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import kotlinx.coroutines.flow.first

class CashUpActivity : AppCompatActivity() {
    lateinit var  binding:ActivityCashUpBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCashUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableBackButton()

        binding.submit.setOnClickListener {
            val cashupDateTime = intent.getStringExtra("CASHUP_DATE_TIME")
            if(binding.checkBox.isChecked){
                val intent = Intent(this@CashUpActivity,CashUpDetailsActivity::class.java)
                 intent.putExtra("CASHUP_DATE_TIME", cashupDateTime)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

                startActivity(intent)
                finish()
            }else{
                Toast.makeText(this,"Please Confirm to Start Cashup",Toast.LENGTH_SHORT).show()
            }
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
}