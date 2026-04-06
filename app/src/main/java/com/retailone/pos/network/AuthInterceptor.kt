package com.retailone.pos.network

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

// Interceptor to add auth token to requests

class AuthInterceptor(val context: Context) : Interceptor {

    private val loginSession = LoginSession.getInstance(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        //requestBuilder.addHeader("Authorization", "Bearer 40|twrZBkQYhJUMIuSyUxGYhjyCkxbLi12GwpKxz3Uo")

       /* CoroutineScope(Dispatchers.Main).launch {
            // Suspend function can be called here
            val token = loginSession.getToken().first()
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }*/

      /*  GlobalScope.launch {
            // Inside a coroutine
            val token = loginSession.getToken().first()
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        */

        val token = runBlocking {
            loginSession.getToken().first()
        }

        requestBuilder.addHeader("Authorization", "Bearer $token")
        requestBuilder.addHeader("Accept", "application/json")

        return chain.proceed(requestBuilder.build())
    }
}

