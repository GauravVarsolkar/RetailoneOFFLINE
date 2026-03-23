package com.retailone.pos.ui.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.retailone.pos.R
import com.retailone.pos.databinding.ActivityForgotPinBinding
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.viewmodels.ForgotPinViewmodel

class ForgotPinActivity : AppCompatActivity() {
    lateinit var binding:ActivityForgotPinBinding
    lateinit var forgotpinviewmodel :ForgotPinViewmodel

    var otpinput = arrayOfNulls<EditText>(6)
    var my_mobileno = ""

    private var timer: CountDownTimer? = null
    private val countdownInterval: Long = 1000 // 1 second
    private val totalTime: Long = 60000 // 60 seconds


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityForgotPinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        forgotpinviewmodel = ViewModelProvider(this)[ForgotPinViewmodel::class.java]

        setOtpEditTextHandler()
        binding.mobileContainer.visibility = View.VISIBLE // initially visible mobile layout


        forgotpinviewmodel.loadingLiveData.observe(this){
            binding.progress.isVisible = it.isProgress

            if(it.isMessage)
                showMessage(it.message)
        }

        forgotpinviewmodel.changepinLivedata.observe(this){
            if(it.status == 1){
                showMessage(it.message)
                val intent = Intent(this@ForgotPinActivity,MPOSLoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()

            }else{
                showMessage(it.message)
            }
        }

        forgotpinviewmodel.verifyLivedata.observe(this){
            if(it.status==1){
                showMessage(it.message)
                binding.otpContainer.visibility = View.GONE
                binding.pinContainer.visibility = View.VISIBLE
            }else{
                showMessage(it.message)
            }
        }

        forgotpinviewmodel.otpLivedata.observe(this){

            if(it.status == 1){
                binding.mobileContainer.visibility = View.GONE
                binding.otpContainer.visibility = View.VISIBLE

                binding.verifyOtp.text = "Enter OTP"
                binding.verifyOtp.isEnabled = false
                binding.mobileinfo.text = "$my_mobileno"
                otpinput[0]?.requestFocus()

                //Start Count down
                startResendTimer()

            }else{
                // mobile no not registered
                showMessage(it.message)
            }
        }



        binding.sendotpBtn.setOnClickListener {
            validateMobile(binding.mobileedit.text.toString())

        }

        binding.verifyOtp.setOnClickListener {
            verifyMyOTP()
        }

        binding.resetpinBtn.setOnClickListener {
            val newpin = binding.pinedit.text.toString()
            val confirmpin = binding.confirmpinedit.text.toString()
            validateNewPin(newpin,confirmpin)
        }

        binding.resendText.setOnClickListener {

            sendOTP(my_mobileno)

        }



    }


    private fun startResendTimer() {
        // Disable the resend button during the timer
        binding.resendText.isVisible = false
        binding.resendValue.isVisible = true

        // Start the countdown timer
        timer = object : CountDownTimer(totalTime, countdownInterval) {
            override fun onTick(millisUntilFinished: Long) {
                // Update the UI with the remaining time
                val secondsRemaining = millisUntilFinished / 1000
                binding.resendValue.text = getString(R.string.resend_timer, secondsRemaining)
            }

            override fun onFinish() {
                // Enable the resend button after the timer finishes
                binding.resendText.isVisible = true
                binding.resendValue.isVisible = false
                binding.resendValue.text = getString(R.string.resend_timer, 0)
            }
        }.start()
    }

    private fun validateNewPin(newpin: String, confirmpin: String) {
        if(newpin.trim().length == 6){
            if(confirmpin == newpin){

                resetNewPin(my_mobileno,newpin)

            }else{
                showMessage("Confirm pin must be same as new pin")
            }
        }else{
            showMessage("Please enter valid pin")
        }
    }

    private fun resetNewPin(myMobileno: String, newpin: String) {
        forgotpinviewmodel.callChangePinApi(myMobileno,newpin)

    }

    private fun verifyMyOTP() {

        val my_otp ="${otpinput[0]?.text}${otpinput[1]?.text}${otpinput[2]?.text}${otpinput[3]?.text}${otpinput[4]?.text}${otpinput[5]?.text}"

        if(my_otp.trim().length==6){
            forgotpinviewmodel.callVerifyOtpApi(my_mobileno,my_otp)
        }else{
            showMessage("Invalid OTP")
        }

    }

    private fun validateMobile(mobileno: String) {

        if(mobileno.isEmpty() || mobileno.isBlank() || mobileno.length!=10){
            showMessage("please enter valid mobile no")
        }else{
            sendOTP(mobileno)
            my_mobileno = mobileno
        }


    }

    private fun sendOTP(mobileno: String) {

        forgotpinviewmodel.callSendOtpApi(this@ForgotPinActivity,mobileno)

    }

    private fun setOtpEditTextHandler() {

        otpinput[0] = binding.otp1
        otpinput[1] = binding.otp2
        otpinput[2] = binding.otp3
        otpinput[3] = binding.otp4
        otpinput[4] = binding.otp5
        otpinput[5] = binding.otp6


        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)

        //by default request focus to first otp
        /* otpinput.get(0)?.requestFocus()*/

        for (i in 0..5) { //Its designed for 6 digit OTP
            otpinput[i]?.addTextChangedListener(object : TextWatcher {

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable) {
                    if (i == 5 && otpinput[i]!!.text.toString().isNotEmpty()) {
                        otpinput[i]!!.clearFocus()
                        //Clears focus when you have entered the last digit of the OTP.

                    } else if (otpinput[i]!!.text.toString().isNotEmpty()) {
                        otpinput[i + 1]!!.requestFocus()
                        //focuses on the next edittext after a digit is entered.
                    }

                    // set Button Enable disabled
                    if (otpinput[0]!!.text.toString().isNotEmpty() && otpinput[1]!!.text.toString().isNotEmpty()
                        && otpinput[2]!!.text.toString().isNotEmpty() && otpinput[3]!!.text.toString().isNotEmpty()
                        && otpinput[4]!!.text.toString().isNotEmpty() && otpinput[5]!!.text.toString().isNotEmpty()
                    ) {
                        inputMethodManager.hideSoftInputFromWindow(binding.verifyOtp.windowToken, 0)
                        binding.verifyOtp.text = "Verify OTP"
                        binding.verifyOtp.isEnabled = true
                    } else {
                        binding.verifyOtp.text = "Enter OTP"
                        binding.verifyOtp.isEnabled = false
                    }
                }
            })
            otpinput[i]?.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
                if (event.action != KeyEvent.ACTION_DOWN) {
                    return@OnKeyListener false
                    // it is because onKeyListener is called twice and this condition is to avoid it.
                }
                if (keyCode == KeyEvent.KEYCODE_DEL &&
                    otpinput[i]!!.text.toString().isEmpty() && i != 0
                ) {
                    //this condition is to handel the delete input by users.
                    otpinput[i - 1]!!.setText("") //Deletes the digit of OTP
                    otpinput[i - 1]!!.requestFocus() //and sets the focus on previous digit
                }
                false
            })
        }
    }


    private fun showMessage(msg: String) {
        Toast.makeText(this@ForgotPinActivity, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        // Cancel the timer to avoid memory leaks
        timer?.cancel()
        super.onDestroy()
    }
}