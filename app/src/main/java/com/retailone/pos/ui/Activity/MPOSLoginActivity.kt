package com.retailone.pos.ui.Activity

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.retailone.pos.databinding.ActivityMposloginBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.TimeoutHelper
import com.retailone.pos.models.LoginModels.LoginResponse
import com.retailone.pos.viewmodels.DashboardViewodel.ProfileAttendanceViewmodel
import com.retailone.pos.viewmodels.MPOSLoginViewmodel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
//Rawanda Code
class MPOSLoginActivity : AppCompatActivity() {
    lateinit var  binding :ActivityMposloginBinding
    lateinit var  loginviewmodel:MPOSLoginViewmodel
    lateinit var  loginSession: LoginSession
    lateinit var  profileAttendanceViewmodel: ProfileAttendanceViewmodel
    private var loginResponse: LoginResponse? = null
    private var yourCoroutineJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMposloginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loginviewmodel = ViewModelProvider(this)[MPOSLoginViewmodel::class.java]
        profileAttendanceViewmodel = ViewModelProvider(this)[ProfileAttendanceViewmodel::class.java]
        loginSession = LoginSession.getInstance(this)


     /*   lifecycleScope.launch {
            // Check if the user is logged in
            val isLoggedIn = loginSession.getLoginStatus().first()

            if (isLoggedIn) {
                navigateToActivity(MPOSDashboardActivity::class.java)
            }

        }*/

        loginviewmodel.loadingLiveData.observe(this){
            binding.progress.isVisible = it.isProgress

            if(it.isMessage)
                showMessage(it.message)
        }



        loginviewmodel.loginLiveData.observe(this){

            if(it.status == 1){
                loginResponse = it
                //store the token for calling profile Api but login status = false
                //then call profile APi to save store id then save loginsession = true (by loginresponse)

                val cashupDateTime = it.cashup_date_time

                CoroutineScope(Dispatchers.IO).launch {
                    loginSession.storeLoginSession(it.data.token,false)
                    loginSession.storeCashupDateTime(it.cashup_date_time.toString())
                    Log.d("LoginSession", "Saving cashup time: ${it.cashup_date_time}")
                    profileAttendanceViewmodel.callUserProfileApi(this@MPOSLoginActivity)
                    //get store Id And Save it
                }

                //navigateToHomepage()
                //showMessage(it.message)
                showMessage("Fetching Store Details...")
            }else{
                showMessage(it.message)
            }
        }

        profileAttendanceViewmodel.loadingLiveData.observe(this){
            binding.progress.isVisible = it.isProgress
            if(it.isMessage)
                showMessage(it.message)
        }

        profileAttendanceViewmodel.userProfileLiveData.observe(this){

          // if store id and login response is not null save the login session
            CoroutineScope(Dispatchers.IO).launch {
                val storeid = it.data.user_details.store_id
                val store_manager_id = it.data.user_details.id


                if(!storeid.isNullOrBlank() && loginResponse!= null){

                   // Timeout helper
                    val timeouthelper = TimeoutHelper(this@MPOSLoginActivity)
                    timeouthelper.saveSessionTimestamp()

                    loginSession.saveStoreID(storeid)
                    loginSession.saveStoreManagerID(store_manager_id.toString()) //storemanager_id
                    loginSession.storeLoginSession(loginResponse!!.data.token,true)
                    showMessage("Login Sucessfull")
                   // navigateToActivity(FetchTOT::class.java,"USER_STATUS" ,"LoggedIn")
                    navigateToHomepage()
                }else{
                    showMessage("User not associated with any store")
                }
            }

        }


        binding.loginBtn.setOnClickListener {
            val userid = binding.mobileedit.text.toString()
            val pin = binding.pinedit.text.toString()
            validateCredential(userid,pin)
        }

        binding.forgotpin.setOnClickListener {
            val intent = Intent(this@MPOSLoginActivity,ForgotPinActivity::class.java)
            startActivity(intent)
        }
       /* binding.biomatric.setOnClickListener {
            val intent = Intent(this@MPOSLoginActivity,MPOSDashboardActivity::class.java)
            startActivity(intent)
        }*/

        binding.forgotpin.paintFlags = Paint.UNDERLINE_TEXT_FLAG
    }

    private fun navigateToHomepage() {
        val intent = Intent(this@MPOSLoginActivity, MPOSDashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun validateCredential(userid: String, pin: String) {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"


        val device_id = getMyDeviceId(this)

        if(userid.isEmpty() || userid.isBlank()){
            showMessage("Please Enter Registered Email id")
        }else if (!userid.matches(emailPattern.toRegex())) {
            showMessage("Please Enter a valid Email id")
        }else if(pin.isEmpty() || pin.isBlank() || pin.trim().length != 6){
            showMessage("Please Enter 6 digit PIN")
        }else{
            login(userid,pin,device_id)
        }

    }


    fun getMyDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    }

    private fun login(userid: String, pin: String, device_id: String) {
       // val intent = Intent(this@MPOSLoginActivity,MPOSDashboardActivity::class.java)
        //startActivity(intent)

        loginviewmodel.callLoginApi(this@MPOSLoginActivity,userid, pin,device_id)

    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this@MPOSLoginActivity, activityClass)
        startActivity(intent)
        finish()
    }


        private fun showMessage(msg: String) {

        yourCoroutineJob = GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MPOSLoginActivity, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun <T> navigateToActivity(target: Class<T>, key: String? = null, value: String? = null) {
        val intent = Intent(this, target)
        if (key != null && value != null) {
            intent.putExtra(key, value) // Add the key-value pair to the intent
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        // Cancel the coroutine job if it's not null
        yourCoroutineJob?.cancel()

        super.onDestroy()
    }

}



