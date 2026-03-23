package com.retailone.pos.network

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Retrofit instance class

class ApiClient {
    private lateinit var apiService: ApiService

    fun getApiService(context: Context): ApiService {
        // Initialize ApiService if not initialized yet
        if (!::apiService.isInitialized) {
            // Use lenient Gson to parse responses that may return arrays or non-strict JSON
            val gson = com.google.gson.GsonBuilder().setLenient().create()
            val retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okhttpClient(context)) // Add our Okhttp client
                .build()

            apiService = retrofit.create(ApiService::class.java)
        }

        return apiService
    }


     // Initialize OkhttpClient with our interceptor

    private fun okhttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .build()
    }


     // Without Token
   fun getApiServiceNoToken():ApiService{

       if (!::apiService.isInitialized) {
           val retrofit = Retrofit.Builder()
               .baseUrl(Constants.BASE_URL)
               .addConverterFactory(GsonConverterFactory.create())
               .build()

           apiService = retrofit.create(ApiService::class.java)
       }

       return apiService

   }

}
