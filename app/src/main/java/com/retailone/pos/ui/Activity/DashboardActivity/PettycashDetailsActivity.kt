package com.retailone.pos.ui.Activity.DashboardActivity

import NumberFormatter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.R
import com.retailone.pos.adapter.PettyCashSummaryAdapter
import com.retailone.pos.adapter.SalesPaymentAdapter
import com.retailone.pos.databinding.ActivityMposloginBinding
import com.retailone.pos.databinding.ActivityPettycashDetailsBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.models.LocalizationModel.LocalizationData
import com.retailone.pos.viewmodels.DashboardViewodel.PettycashDetailsViewmodel
import com.retailone.pos.viewmodels.MPOSLoginViewmodel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class PettycashDetailsActivity : AppCompatActivity() {

    lateinit var  binding : ActivityPettycashDetailsBinding
    lateinit var  pettycash_viewmodel: PettycashDetailsViewmodel
    lateinit var  loginSession: LoginSession
    lateinit var  pettycash_summery_adapter: PettyCashSummaryAdapter
    lateinit var  localizationData: LocalizationData


    var storeid = ""
    var store_manager_id = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPettycashDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pettycash_viewmodel = ViewModelProvider(this)[PettycashDetailsViewmodel::class.java]
        loginSession = LoginSession.getInstance(this)

        localizationData = LocalizationHelper(this).getLocalizationData()



        lifecycleScope.launch {
            storeid = LoginSession.getInstance(this@PettycashDetailsActivity).getStoreID().first()
                .toString()
            store_manager_id =
                LoginSession.getInstance(this@PettycashDetailsActivity).getStoreManagerID().first()
                    .toString()


            pettycash_viewmodel.callPettycashDataApi(storeid,this@PettycashDetailsActivity)

        }


        enableBackButton()
        prepareSalesPaymentRCV()

        getCurrentDate()


        pettycash_viewmodel.loadingLiveData.observe(this){
            binding.progress.isVisible = it.isProgress
            if(it.isMessage)
                showMessage(it.message)
        }


       // pettycash_viewmodel.callPettycashDataApi("1",this)

        pettycash_viewmodel.pettycash_liveData.observe(this){
          //  showMessage(it.petty_cash_summary.size.toString())

            if(it.status==1){
                val formattedPricercv = NumberFormatter().formatPrice(it.pettycash_total_balance .toString(),localizationData)
                binding.balanceValue.text = formattedPricercv

                val pettycash_list = it.petty_cash_summary

                if(pettycash_list.isNotEmpty()){
                    pettycash_summery_adapter = PettyCashSummaryAdapter(pcList = pettycash_list,this)
                    binding.pettycashSummaryRcv.adapter = pettycash_summery_adapter
                    binding.pettycashSummaryRcv.isVisible = true
                    binding.noDataFound.isVisible = false
                }else{
                    binding.pettycashSummaryRcv.isVisible = false
                    binding.noDataFound.isVisible = true
                }
            }


            }

    }



    private fun getCurrentDate() {
        val zone = localizationData.timezone
        lateinit var timezone :String

        if (zone == "IST"){
            timezone = "Asia/Kolkata"
        }else if(zone == "CAT"){
            timezone = "Africa/Lusaka"
        }else{
            timezone = "Africa/Lusaka"
        }

        val calendar = Calendar.getInstance()

        // Set the time zone to Zambia (Africa/Lusaka)
        val zambiaTimeZone = TimeZone.getTimeZone(timezone)
        calendar.timeZone = zambiaTimeZone

        val currentDateTime = calendar.time

        val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        dateFormat.timeZone = zambiaTimeZone

        val formattedDateTime = dateFormat.format(currentDateTime)

        binding.dateValue.text = Editable.Factory.getInstance().newEditable(formattedDateTime)

    }


    private fun prepareSalesPaymentRCV() {
        binding.pettycashSummaryRcv.apply {
            layoutManager = LinearLayoutManager(this@PettycashDetailsActivity,
                RecyclerView.VERTICAL,false)
            //adapter = sales_adapter
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
        Toast.makeText(this@PettycashDetailsActivity, msg, Toast.LENGTH_SHORT).show()
    }
}