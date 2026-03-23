package com.retailone.pos.ui.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.retailone.pos.databinding.ActivitySplashScreenBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashScreenActivity : AppCompatActivity() {
    lateinit var binding : ActivitySplashScreenBinding
    // val activityScope = CoroutineScope(Dispatchers.Main)
    lateinit var  loginSession: LoginSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loginSession = LoginSession.getInstance(this)


        // Start a coroutine in the lifecycle scope
        lifecycleScope.launch {
            delay(1000)
//            // Check if the user is logged in
            val isLoggedIn = loginSession.getLoginStatus().first()

            if (isLoggedIn) {
                navigateToActivity(MPOSDashboardActivity::class.java )
            } else {
                navigateToActivity(MPOSLoginActivity::class.java)
            }

            // navigateToActivity(DummyScannerActivity::class.java);

        }


        /*  val thread = Thread {
              Thread.sleep(2000)

              val intent = Intent(this@SplashScreenActivity, MPOSLoginActivity::class.java)
              startActivity(intent)
              finish()
          }

          thread.start()*/


        /* activityScope.launch {
             delay(2000)
             val i = Intent(this@SplashScreenActivity, MPOSLoginActivity::class.java)
             startActivity(i)
             finish()
         }*/

    }


    private fun <T> navigateToActivity(target: Class<T>, key: String? = null, value: String? = null) {
        val intent = Intent(this, target)
        if (key != null && value != null) {
            intent.putExtra(key, value) // Add the key-value pair to the intent
        }
        startActivity(intent)
    }


    override fun onPause() {
        // activityScope.cancel()
        super.onPause()
    }
}