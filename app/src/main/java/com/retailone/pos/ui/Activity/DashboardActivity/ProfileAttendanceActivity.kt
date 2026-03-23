package com.retailone.pos.ui.Activity.DashboardActivity

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
import com.retailone.pos.adapter.AttendanceAdapter
import com.retailone.pos.databinding.ActivityProfileAttendanceBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.models.CashupModel.CashupDetails.CashupDetailsReq
import com.retailone.pos.viewmodels.DashboardViewodel.ProfileAttendanceViewmodel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileAttendanceActivity : AppCompatActivity() {
    lateinit var binding:ActivityProfileAttendanceBinding
    lateinit var  attendanceViewmodel: ProfileAttendanceViewmodel
    lateinit var  attendance_adapter:AttendanceAdapter
    lateinit var  adapter: AttendanceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        attendanceViewmodel = ViewModelProvider(this)[ProfileAttendanceViewmodel::class.java]

        enableBackButton()

        prepareAttendanceRCV()

        lifecycleScope.launch {
            val storeid = LoginSession.getInstance(this@ProfileAttendanceActivity).getStoreID().first().toString()
            val store_manager_id = LoginSession.getInstance(this@ProfileAttendanceActivity).getStoreManagerID().first().toString()
            attendanceViewmodel.callMonthlyattendanceApi( this@ProfileAttendanceActivity,store_manager_id )

        }

        attendanceViewmodel.attendance_LiveData.observe(this){
            if(it.monthly_attendance.isNotEmpty()){
                attendance_adapter = AttendanceAdapter(it.monthly_attendance,this)
                binding.attendanceRcv.adapter= attendance_adapter

            }else{
                showMessage("Attendance list not found")
            }

        }

        attendanceViewmodel.callUserProfileApi(this@ProfileAttendanceActivity)

        attendanceViewmodel.loadingLiveData.observe(this){
            binding.progress.isVisible = it.isProgress

            if(it.isMessage)
                showMessage(it.message)
        }



        attendanceViewmodel.userProfileLiveData.observe(this){

           if(it.status == 1){
               val userDetails = it.data.user_details

               binding.nametext.text = userDetails.first_name ?:""
               binding.emailtext.text = userDetails.email ?:""
               binding.phonetext.text = userDetails.contact_no ?:""
               binding.storenametext.text = userDetails.store_name ?:""
           }else{
               showMessage(it.message)
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
            .into(binding.image)    }

    private fun showMessage(msg: String) {
        Toast.makeText(this@ProfileAttendanceActivity, msg, Toast.LENGTH_SHORT).show()
    }

    private fun prepareAttendanceRCV() {

        binding.attendanceRcv.apply {
            layoutManager = LinearLayoutManager(this@ProfileAttendanceActivity,RecyclerView.VERTICAL,false)
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
}