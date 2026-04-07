package com.retailone.pos.viewmodels.DashboardViewodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.retailone.pos.models.AddNewCustomerModel.AddNewCustReq
import com.google.gson.reflect.TypeToken
import com.retailone.pos.models.AddNewCustomerModel.AddNewCustRes
import com.retailone.pos.models.GetCustomerModel.getCustomerReq
import com.retailone.pos.models.GetCustomerModel.getCustomerRes
import com.retailone.pos.models.PointofsaleModel.PointOfSaleItem
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.PosAddToCartReq
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.PosAddToCartRes
import com.retailone.pos.models.PointofsaleModel.PosSaleModel.PosSaleReq
import com.retailone.pos.models.PointofsaleModel.PosSaleModel.PosSaleRes
import com.retailone.pos.models.PointofsaleModel.SearchStoreProBarcodeModel.SearchStoreProBarcodeReq
import com.retailone.pos.models.PointofsaleModel.SearchStoreProBarcodeModel.SearchStoreProBarcodeRes
import com.retailone.pos.models.PointofsaleModel.SearchStroreProModel.SearchStoreProReq
import com.retailone.pos.models.PointofsaleModel.SearchStroreProModel.SearchStoreProRes
import com.retailone.pos.models.PointofsaleModel.toPatchedJson
import com.retailone.pos.models.PosSalesDetailsModel.PosSalesDetails
import com.retailone.pos.models.ProgressModel.ProgressData
import com.retailone.pos.network.ApiClient
import com.retailone.pos.network.SingleLiveEvent
import com.retailone.pos.localstorage.SharedPreference.SharedPrefHelper
import com.retailone.pos.localstorage.SharedPreference.CustomerSessionHelper
import com.retailone.pos.utils.NetworkUtils
import com.retailone.pos.localstorage.SharedPreference.CustomerLocalHelper
import com.retailone.pos.localstorage.SharedPreference.InventoryStockHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.lifecycle.viewModelScope
import com.retailone.pos.repository.PosSaleRepository
import com.retailone.pos.localstorage.SharedPreference.OfflineLoginHelper
import com.retailone.pos.models.GetCustomerModel.CustomerData
import com.retailone.pos.models.PosSalesDetailsModel.ReceiptTypeResponse
import com.retailone.pos.localstorage.RoomDB.ReceiptTypeEntity
import com.retailone.pos.localstorage.RoomDB.PosDatabase
import com.retailone.pos.models.PosSalesDetailsModel.ReceiptType
import com.retailone.pos.repository.PosProductRepository
import kotlinx.coroutines.launch
import org.json.JSONObject


class PointofSaleViewmodel: ViewModel() {


    val loading = MutableLiveData<ProgressData>()
    val loadingLiveData : LiveData<ProgressData>
        get() = loading


    val storeProSearchdata = MutableLiveData<SearchStoreProRes>()
    val storeProSearchLivedata : LiveData<SearchStoreProRes>
        get() = storeProSearchdata

    val storeProSearchBarcodedata = MutableLiveData<SearchStoreProBarcodeRes>()
    val storeProSearchBarcodeLivedata : LiveData<SearchStoreProBarcodeRes>
        get() =  storeProSearchBarcodedata

    val posAddtocartData = MutableLiveData<PosAddToCartRes>()
    val posAddtocartLivedata : LiveData<PosAddToCartRes>
        get() = posAddtocartData

    val posSaleData = MutableLiveData<PosSalesDetails>()
    val posSaleLivedata : LiveData<PosSalesDetails>
        get() = posSaleData


    val addNewCustData = MutableLiveData<AddNewCustRes>()
    val addNewCustLivedata : LiveData<AddNewCustRes>
        get() = addNewCustData



    // Using SingleLiveEvent instead of MutableLiveData
    private val get_customerdata = SingleLiveEvent<getCustomerRes>()

    // Exposing the LiveData for observers
    val get_customer_liveData: LiveData<getCustomerRes>
        get() = get_customerdata

    private var repository: PosProductRepository? = null

    private fun getRepository(context: Context): PosProductRepository {
        if (repository == null) {
            repository = PosProductRepository(context)
        }
        return repository!!
    }

    private var saleRepository: PosSaleRepository? = null

    private fun getSaleRepository(context: Context): PosSaleRepository {
        if (saleRepository == null) {
            saleRepository = PosSaleRepository(context)
        }
        return saleRepository!!
    }

    // Function to update the value
    fun updateCustomerData(customerData: getCustomerRes) {
        get_customerdata.value = customerData
    }

    private val receiptTypeData = MutableLiveData<ReceiptTypeResponse>()
    val receiptTypeLiveData: LiveData<ReceiptTypeResponse>
        get() = receiptTypeData

    // Helper function to extract error message from Response
    private fun getErrorMessage(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (errorBody.isNullOrEmpty()) return "Something went wrong"

            val jsonObject = JSONObject(errorBody)

            // Check for your custom error format with status = 0
            val status = jsonObject.optInt("status", -1)
            if (status == 0) {
                // Try to get message first, fallback to data field
                val message = jsonObject.optString("message", "")
                val data = jsonObject.optString("data", "")

                return when {
                    message.isNotEmpty() && message != "null" -> message
                    data.isNotEmpty() && data != "null" -> data
                    else -> "An error occurred"
                }
            }

            // Fallback to standard error message
            jsonObject.optString("message", "Something went wrong")
        } catch (e: Exception) {
            Log.e("ErrorParsing", "Failed to parse error: ${e.message}")
            "Something went wrong. Please try again."
        }
    }

    // Overload for errorBody string
    private fun getErrorMessage(errorBody: String?): String {
        return try {
            if (errorBody.isNullOrEmpty()) {
                Log.e("ErrorParsing", "Error body is null or empty")
                return "Something went wrong"
            }

            Log.d("ErrorParsing", "Parsing error body: $errorBody")
            val jsonObject = JSONObject(errorBody)

            // Try to parse as PosSalesDetails structure
            val status = jsonObject.optInt("status", -1)
            val message = jsonObject.optString("message", "")
            val data = jsonObject.optString("data", "")

            Log.d("ErrorParsing", "status=$status, message='$message', data='$data'")

            when {
                message.isNotEmpty() && message != "null" -> message
                data.isNotEmpty() && data != "null" -> data
                else -> "An error occurred"
            }
        } catch (e: Exception) {
            Log.e("ErrorParsing", "Failed to parse error: ${e.message}", e)
            "Something went wrong. Please try again."
        }
    }



    fun callSearchStoreProductApi(
        searchname: String,
        storeid: Int,
        customerid: Int,
        context: Context
    ) {
        loading.postValue(ProgressData(isProgress = true))

        // Launch coroutine to collect from repository Flow
        viewModelScope.launch {
            try {
                val repo = getRepository(context)

                // Collect from Flow (this will update automatically when Room data changes)
                repo.searchProducts(
                    searchQuery = searchname,
                    storeId = storeid,
                    customerId = customerid,
                    onError = { errorMsg ->
                        // This is called if API sync fails (but local data still works)
                        Log.d("PosViewModel", "Sync error: $errorMsg")
                    }
                ).collect { products ->
                    // Update LiveData with cached data (will update again if API sync brings new data)
                    storeProSearchdata.postValue(
                        SearchStoreProRes(
                            status = 1,
                            message = if (NetworkUtils.isInternetAvailable(context)) "Online" else "Offline mode",
                            data = products
                        )
                    )
                    loading.postValue(ProgressData(isProgress = false))
                }

            } catch (e: Exception) {
                loading.postValue(
                    ProgressData(
                        isProgress = false,
                        isMessage = true,
                        message = "Error: ${e.message}"
                    )
                )
            }
        }
    }


    fun callSearchStoreProductBarcodeApi( searchname: String,storeid:Int,customerid:Int,context: Context){
        loading.postValue(ProgressData(isProgress = true))
        Log.d("request", searchname+storeid+customerid)
        val requestObj = SearchStoreProBarcodeReq(
            customer_id = customerid,
            search_string = searchname,
            store_id = storeid
        )

        // ✅ Print Request JSON
        val requestJson = Gson().toJson(requestObj)
        Log.d("🔍 SearchAPIRequest", requestJson)
        println("🔍 Final Request JSON: $requestJson")
        ApiClient().getApiService(context).searchStoreProductBarcode(SearchStoreProBarcodeReq(customerid,searchname,storeid)).enqueue(object :
            Callback<SearchStoreProBarcodeRes> {
            override fun onResponse(call: Call<SearchStoreProBarcodeRes>, response: Response<SearchStoreProBarcodeRes>) {

                Log.d("xxx", Gson().toJson(response.body()))
                // Log.d("yyy", Gson().toJson(SearchStoreProBarcodeReq(customerid,searchname,storeid)))
                Log.d("APIResponse", "Status Code: ${response.code()}")
                Log.d("APIResponse", "Body: ${Gson().toJson(response.body())}")
                Log.d("APIRESPONSE", "Body: ${Gson().toJson(response.body())}")
                if(response.isSuccessful && response.body()!=null){
                    storeProSearchBarcodedata.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<SearchStoreProBarcodeRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
            }
        })
    }


    fun callAddtoCartPosApi(posAddToCartReq: PosAddToCartReq,context: Context){
        loading.postValue(ProgressData(isProgress = true))
        //vhdvdbvdbvn
        //called requst and response
        val requestJson = GsonBuilder().setPrettyPrinting().create().toJson(posAddToCartReq)
        Log.d("📝 AddToCart Request JSON", requestJson)
        println("📝 Final Request JSON:\n$requestJson")
        if (posAddToCartReq == null) {
            Log.e("🛑 RequestError", "posAddToCartReq is NULL")
            return
        }
        ApiClient().getApiService(context).addToCartPos(posAddToCartReq).enqueue(object :
            Callback<PosAddToCartRes> {
            override fun onResponse(call: Call<PosAddToCartRes>, response: Response<PosAddToCartRes>) {
                Log.d("response:new api", Gson().toJson(response.body()))
                // Log.d("yyy", Gson().toJson(SearchStoreProBarcodeReq(customerid,searchname,storeid)))
                Log.d("APIResponse new ", "Status Code: ${response.code()}")
                Log.d("APIResponse new ", "Body: ${Gson().toJson(response.body())}")

                if(response.isSuccessful && response.body()!=null){
                    posAddtocartData.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))
                }
            }

            override fun onFailure(call: Call<PosAddToCartRes>, t: Throwable) {
                Log.d("xxx",t.message.toString())
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something pos Went Wrong"))
            }
        })
    }

    /**
     * NEW: Offline-first sale submission
     * Automatically handles online/offline mode
     */
    fun callposSaleApiPatched(req: PosSaleReq, ctx: Context) {
        loading.postValue(ProgressData(isProgress = true))

        viewModelScope.launch {
            try {
                val repo = getSaleRepository(ctx)

                repo.submitSale(
                    saleReq = req,
                    onSuccess = { response ->
                        // Success (either from API or saved offline)
                        loading.postValue(ProgressData(isProgress = false))
                        posSaleData.postValue(response)
                    },
                    onError = { errorMsg ->
                        // Error (e.g., duplicate invoice)
                        loading.postValue(
                            ProgressData(
                                isProgress = false,
                                isMessage = true,
                                message = errorMsg
                            )
                        )
                    }
                )

            } catch (e: Exception) {
                loading.postValue(
                    ProgressData(
                        isProgress = false,
                        isMessage = true,
                        message = "Error: ${e.message}"
                    )
                )
            }
        }
    }

    fun callAddNewCustApi( addNewCustReq: AddNewCustReq,context: Context){
        loading.postValue(ProgressData(isProgress = true))

        ApiClient().getApiService(context).addNewCustAPI(addNewCustReq).enqueue(object :
            Callback<AddNewCustRes> {
            override fun onResponse(call: Call<AddNewCustRes>, response: Response<AddNewCustRes>) {

                if(response.isSuccessful && response.body()!=null){
                    addNewCustData.postValue(response.body())
                    loading.postValue(ProgressData(isProgress = false))
                }else{
                    //loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Failed to fetch data, Try again" ))

                    //Statically Added to avoid 409 conflict error in this API
                    loading.postValue(ProgressData(isProgress = false,isMessage = true, message ="Customer Already Registered" ))
                }
            }

            override fun onFailure(call: Call<AddNewCustRes>, t: Throwable) {
                loading.postValue(ProgressData(isProgress = false,isMessage = true, message = "Something Went Wrong"))
                //Log.d("x1",t.message.toString())
                // Log.d("x1",Gson().toJson(addNewCustReq))
            }
        })
    }


    fun callGetCustomerDetailsApi(
        getCustomerReq: getCustomerReq,
        context: Context
    ) {

        loading.postValue(ProgressData(isProgress = true))

        val offlineLoginHelper = OfflineLoginHelper(context.applicationContext)

        // =====================================================
        // ✅ STEP 1: OFFLINE LOGIN (SEARCH ALL CACHED CUSTOMERS)
        // =====================================================
        if (!NetworkUtils.isInternetAvailable(context)) {

            loading.postValue(ProgressData(isProgress = false))

            val inputMobile = getCustomerReq.mobile_no
            val inputTpin = getCustomerReq.tin_tpin_no
            val customerHelper = CustomerLocalHelper(context.applicationContext)

            val customers = customerHelper.getCustomers()
            val matchedCustomer = if (!inputMobile.isNullOrEmpty()) {
                customers.find { it.mobile_no == inputMobile }
            } else if (!inputTpin.isNullOrEmpty()) {
                customers.find { it.tin_tpin_no == inputTpin }
            } else null

            if (matchedCustomer == null) {
                loading.postValue(
                    ProgressData(
                        isMessage = true,
                        message = "Customer not found in offline records. Please login online once."
                    )
                )
                return
            }

            updateCustomerData(
                getCustomerRes(
                    status = 1,
                    message = "Offline login success",
                    data = matchedCustomer
                )
            )
            return
        }

        // =====================================================
        // ✅ STEP 2: ONLINE LOGIN (SOURCE OF TRUTH)
        // =====================================================
        ApiClient().getApiService(context)
            .getCustomerInfoAPI(getCustomerReq)
            .enqueue(object : Callback<getCustomerRes> {

                override fun onResponse(
                    call: Call<getCustomerRes>,
                    response: Response<getCustomerRes>
                ) {

                    loading.postValue(ProgressData(isProgress = false))

                    if (response.isSuccessful && response.body() != null) {

                        val customer = response.body()!!.data

                        OfflineLoginHelper(context).saveLogin(
                            customerId = customer.id,
                            mobile = customer.mobile_no
                        )

                        // ✅ SAVE CUSTOMER FOR OFFLINE USE
                        CustomerLocalHelper(context.applicationContext)
                            .saveCustomer(customer)

                        updateCustomerData(response.body()!!)


                    } else {
                        loading.postValue(
                            ProgressData(
                                isMessage = true,
                                message = "Failed to fetch data, Try again"
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<getCustomerRes>, t: Throwable) {
                    loading.postValue(
                        ProgressData(
                            isProgress = false,
                            isMessage = true,
                            message = "Something went wrong"
                        )
                    )
                }
            })
    }
    fun callGetReceiptTypesApi(context: Context) {
        loading.postValue(ProgressData(isProgress = true))

        val db = PosDatabase.getDatabase(context)
        val dao = db.receiptTypeDao()

        // =====================================================
        // ✅ STEP 1: CHECK OFFLINE RECORDS FIRST IF NO INTERNET
        // =====================================================
        if (!NetworkUtils.isInternetAvailable(context)) {
            Log.d("ReceiptType_API", "Offline mode: Loading from local DB")
            viewModelScope.launch {
                val localTypes = dao.getAllReceiptTypes()
                if (localTypes.isNotEmpty()) {
                    val response = ReceiptTypeResponse(
                        status = 1,
                        message = "Loaded from offline cache",
                        data = localTypes.map { 
                            ReceiptType(it.id, it.code, it.name, it.useYn, it.created_at, it.updated_at) 
                        }
                    )
                    receiptTypeData.postValue(response)
                    loading.postValue(ProgressData(isProgress = false))
                } else {
                    loading.postValue(
                        ProgressData(
                            isProgress = false,
                            isMessage = true,
                            message = "No receipt types cached. Please connect and login once."
                        )
                    )
                }
            }
            return
        }

        // =====================================================
        // ✅ STEP 2: ONLINE FETCH & CACHE
        // =====================================================
        Log.d("ReceiptType_API", "Fetching receipt types from API...")

        ApiClient().getApiService(context).getReceiptTypes()
            .enqueue(object : Callback<ReceiptTypeResponse> {
                override fun onResponse(
                    call: Call<ReceiptTypeResponse>,
                    response: Response<ReceiptTypeResponse>
                ) {
                    Log.d("ReceiptType_API", "Status Code: ${response.code()}")

                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        receiptTypeData.postValue(body)
                        loading.postValue(ProgressData(isProgress = false))

                        // ✅ CACHE TO LOCAL DB
                        viewModelScope.launch {
                            val entities = body.data.map {
                                ReceiptTypeEntity(it.id, it.code, it.name, it.useYn, it.created_at, it.updated_at)
                            }
                            dao.clearAll()
                            dao.insertReceiptTypes(entities)
                            Log.d("ReceiptType_API", "Cached ${entities.size} receipt types to local DB")
                        }
                    } else {
                        // API failure, try loading from local DB as fallback
                        viewModelScope.launch {
                            val localTypes = dao.getAllReceiptTypes()
                            if (localTypes.isNotEmpty()) {
                                val fallbackResponse = ReceiptTypeResponse(
                                    status = 1,
                                    message = "API failed. Loaded from offline cache",
                                    data = localTypes.map { 
                                        ReceiptType(it.id, it.code, it.name, it.useYn, it.created_at, it.updated_at) 
                                    }
                                )
                                receiptTypeData.postValue(fallbackResponse)
                                loading.postValue(ProgressData(isProgress = false))
                            } else {
                                loading.postValue(
                                    ProgressData(
                                        isProgress = false,
                                        isMessage = true,
                                        message = "Failed to load receipt types"
                                    )
                                )
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ReceiptTypeResponse>, t: Throwable) {
                    Log.e("ReceiptType_API", "Failure: ${t.localizedMessage}")
                    
                    // Fallback to local DB on network failure
                    viewModelScope.launch {
                        val localTypes = dao.getAllReceiptTypes()
                        if (localTypes.isNotEmpty()) {
                            val fallbackResponse = ReceiptTypeResponse(
                                status = 1,
                                message = "Network error. Loaded from offline cache",
                                data = localTypes.map { 
                                    ReceiptType(it.id, it.code, it.name, it.useYn, it.created_at, it.updated_at) 
                                }
                            )
                            receiptTypeData.postValue(fallbackResponse)
                            loading.postValue(ProgressData(isProgress = false))
                        } else {
                            loading.postValue(
                                ProgressData(
                                    isProgress = false,
                                    isMessage = true,
                                    message = "Network error: ${t.localizedMessage}"
                                )
                            )
                        }
                    }
                }
            })
    }

}
