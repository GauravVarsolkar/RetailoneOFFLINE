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
import com.retailone.pos.repository.PosProductRepository
import kotlinx.coroutines.launch


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
        // ✅ STEP 1: OFFLINE LOGIN (OPTION 2 – LAST LOGIN ONLY)
        // =====================================================
        if (!NetworkUtils.isInternetAvailable(context)) {

            loading.postValue(ProgressData(isProgress = false))

            val inputMobile = getCustomerReq.mobile_no
                ?: getCustomerReq.tin_tpin_no

            val offlineLoginHelper = OfflineLoginHelper(context.applicationContext)
            val customerHelper = CustomerLocalHelper(context.applicationContext)

            if (!offlineLoginHelper.canLoginOffline(inputMobile)) {
                loading.postValue(
                    ProgressData(
                        isMessage = true,
                        message = "Offline login allowed only for last logged-in user"
                    )
                )
                return
            }

            val lastCustomer = customerHelper.getLastLoggedInCustomer()

            if (lastCustomer == null) {
                loading.postValue(
                    ProgressData(
                        isMessage = true,
                        message = "Offline data not available. Please login online once."
                    )
                )
                return
            }

            updateCustomerData(
                getCustomerRes(
                    status = 1,
                    message = "Offline login success",
                    data = lastCustomer
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

                        // ✅ SAVE FULL REAL CUSTOMER
                        CustomerLocalHelper(context.applicationContext)
                            .saveLastLoggedInCustomer(customer)

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
}
