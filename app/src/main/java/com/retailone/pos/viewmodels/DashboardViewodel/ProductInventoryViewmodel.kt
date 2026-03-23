package com.retailone.pos.viewmodels.DashboardViewodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.retailone.pos.models.ProductInventoryModel.PiChildData
import com.retailone.pos.models.ProductInventoryModel.PiData
import com.retailone.pos.models.ProductInventoryModel.PiParentData
import com.retailone.pos.models.ProductInventoryModel.PiRequestModel.ProductInventoryRequest
import com.retailone.pos.models.ProductInventoryModel.PiResponseModel.ProductInventoryResponse
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.models.StockRequisitionModel.StockSearchModel.StockSearchReq
import com.retailone.pos.models.StockRequisitionModel.StockSearchModel.StockSearchRes
import com.retailone.pos.models.UserProfileModels.UserProfileResponse
import com.retailone.pos.network.ApiClient
import com.retailone.pos.utils.NetworkUtils
import kotlinx.coroutines.launch
import com.retailone.pos.localstorage.RoomDB.toEntities
import com.retailone.pos.localstorage.RoomDB.toInventoryResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProductInventoryViewmodel:ViewModel() {


    val pi_data = MutableLiveData<PiData>()
    val pi_LiveData: LiveData<PiData>
        get() = pi_data

    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData: LiveData<ProgressData>
        get() = loading

    val inventorydata = MutableLiveData<ProductInventoryResponse>()
    val inventoryLiveData: LiveData<ProductInventoryResponse>
        get() = inventorydata


    fun getPiData() {

        val child_list = arrayListOf<PiChildData>()
        child_list.add(PiChildData("12377", "10", "ZK 120", "sep/2024"))
        child_list.add(PiChildData("12678", "16", "ZK 190", "oct/2024"))
        child_list.add(PiChildData("12300", "70", "ZK 129", "jan/2024"))

        val parent_list = arrayListOf<PiParentData>()
        parent_list.add(PiParentData("MM Gold Premium", "1l", "30", "ml", child_list))
        parent_list.add(PiParentData("MM Gold Premium", "1l", "30", "ml", child_list))
        parent_list.add(PiParentData("MM Gold Premium", "1l", "30", "ml", child_list))
        parent_list.add(PiParentData("MM Gold Premium", "1l", "30", "ml", child_list))


        val data = PiData(true, parent_list)
        pi_data.postValue(data)


    }

//    fun callProductInventoryApi( store_id: Int,context: Context){
//        loading.postValue(ProgressData(isProgress = true))
//        ApiClient().getApiService(context).getProductInventory(ProductInventoryRequest(store_id)).enqueue(object :
//            Callback<ProductInventoryResponse> {
//            override fun onResponse(call: Call<ProductInventoryResponse>, response: Response<ProductInventoryResponse>) {
//                Log.d("categorySubmitResponse", response.body().toString())
//
//                if(response.isSuccessful && response.body()!=null){
//                    inventorydata.postValue(response.body())
//                    loading.postValue(ProgressData(isProgress = false))
//                }else{
//                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
//                }
//            }
//
//            override fun onFailure(call: Call<ProductInventoryResponse>, t: Throwable) {
//                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
//            }
//        })
//    }
//}

    fun callProductInventoryApi(store_id: Int, context: Context) {
        loading.postValue(ProgressData(isProgress = true))

        // ✅ Check if online or offline
        if (!NetworkUtils.isInternetAvailable(context)) {
            // OFFLINE MODE: Load from local database
            loadInventoryFromCache(store_id, context)
            return
        }

        // ONLINE MODE: Fetch from API
        val tag = "ProductInventoryApi"
        val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()

        val requestBody = ProductInventoryRequest(store_id)
        Log.d(tag, "➡️ Request body sending to API:\n${gson.toJson(requestBody)}")

        ApiClient().getApiService(context)
            .getProductInventory(requestBody)
            .enqueue(object : Callback<ProductInventoryResponse> {

                override fun onResponse(
                    call: Call<ProductInventoryResponse>,
                    response: Response<ProductInventoryResponse>
                ) {
                    Log.d(tag, "✅ onResponse: isSuccessful=${response.isSuccessful}, code=${response.code()}")

                    if (response.isSuccessful && response.body() != null) {
                        val responseBody = response.body()!!
                        Log.d(tag, "⬅️ Response body from API:\n${gson.toJson(responseBody)}")

                        // ✅ Cache the response in Room
                        cacheInventoryData(store_id, responseBody, context)

                        inventorydata.postValue(responseBody)
                        loading.postValue(ProgressData(isProgress = false))

                    } else {
                        val errorBodyString = try {
                            response.errorBody()?.string()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }

                        Log.e(tag, "❌ API error. code=${response.code()}, message=${response.message()}, errorBody=$errorBodyString")

                        loading.postValue(
                            ProgressData(
                                isProgress = false,
                                isMessage = true,
                                message = "Failed to fetch data, Try again"
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<ProductInventoryResponse>, t: Throwable) {
                    Log.e(tag, "❌ onFailure: ${t.localizedMessage}", t)

                    // ✅ API failed, try loading from cache as fallback
                    loadInventoryFromCache(store_id, context)
                }
            })
    }

    /**
     * ✅ Cache API response in Room database
     */
    private fun cacheInventoryData(storeId: Int, response: ProductInventoryResponse, context: Context) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val database = com.retailone.pos.localstorage.RoomDB.PosDatabase.getDatabase(context)
                val dao = database.productInventoryDao()

                // ✅ Use extension function correctly
                val entities = response.toEntities(storeId)

                // Clear old data and insert new
                dao.clearStoreInventory(storeId)
                dao.insertInventoryItems(entities)

                Log.d("ProductInventoryVM", "✅ Cached ${entities.size} inventory items")

            } catch (e: Exception) {
                Log.e("ProductInventoryVM", "❌ Error caching inventory: ${e.message}", e)
            }
        }
    }


    /**
     * ✅ Load inventory from local database
     */
    private fun loadInventoryFromCache(storeId: Int, context: Context) {
        Log.d("ProductInventoryVM", "🔍 loadInventoryFromCache called for storeId=$storeId")

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val database = com.retailone.pos.localstorage.RoomDB.PosDatabase.getDatabase(context)
                val dao = database.productInventoryDao()

                Log.d("ProductInventoryVM", "🔍 Starting to collect inventory from DB...")

                // Get count first to debug
                val count = dao.getInventoryCount(storeId)
                Log.d("ProductInventoryVM", "🔍 Inventory count in DB: $count")

                // Collect Flow to get real-time updates
                dao.getInventoryByStore(storeId).collect { entities ->

                    Log.d("ProductInventoryVM", "🔍 Collected ${entities.size} entities from Flow")

                    if (entities.isNotEmpty()) {
                        // Transform to ProductInventoryResponse format
                        val response = entities.toInventoryResponse()

                        Log.d("ProductInventoryVM", "🔍 Mapped to ${response.data.size} categories")

                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            inventorydata.postValue(response)
                            loading.postValue(ProgressData(isProgress = false))
                        }

                        Log.d("ProductInventoryVM", "✅ Posted inventory data to LiveData")
                    } else {
                        Log.d("ProductInventoryVM", "❌ No inventory found in database")

                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            loading.postValue(
                                ProgressData(
                                    isProgress = false,
                                    isMessage = true,
                                    message = "No offline data available. Please connect to internet."
                                )
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("ProductInventoryVM", "❌ Error loading from cache: ${e.message}", e)
                e.printStackTrace()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    loading.postValue(
                        ProgressData(
                            isProgress = false,
                            isMessage = true,
                            message = "Error loading offline data: ${e.message}"
                        )
                    )
                }
            }
        }
    }

}
