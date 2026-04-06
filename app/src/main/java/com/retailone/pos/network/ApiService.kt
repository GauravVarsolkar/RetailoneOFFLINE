package com.retailone.pos.network

import StockReturnResponse
import com.retailone.pos.models.AddNewCustomerModel.AddNewCustReq
import com.retailone.pos.models.AddNewCustomerModel.AddNewCustRes
import com.retailone.pos.models.AttendanceModel.MonthlyAttendanceReq
import com.retailone.pos.models.AttendanceModel.MonthlyAttendanceRes
import com.retailone.pos.models.BarcodeModel.StockSearchBarcodeModel.StockSearchBarcodeReq
import com.retailone.pos.models.BarcodeModel.StockSearchBarcodeModel.StockSearchBarcodeRes
import com.retailone.pos.models.CashupModel.CashupDetails.CashupDetailsReq
import com.retailone.pos.models.CashupModel.CashupDetails.CashupDetailsRes
import com.retailone.pos.models.CashupModel.CashupSubmit.CashupSubmitReq
import com.retailone.pos.models.CashupModel.CashupSubmit.CashupSubmitRes
import com.retailone.pos.models.CashupModel.SendOTP.SendOtpReq
import com.retailone.pos.models.CashupModel.SendOTP.SendOtpRes
import com.retailone.pos.models.CashupModel.VerifyOTP.VerifyOtpReq
import com.retailone.pos.models.CashupModel.VerifyOTP.VerifyOtpRes
import com.retailone.pos.models.ChangePinModel.ChangePinRequest
import com.retailone.pos.models.ChangePinModel.ChangePinResponse
import com.retailone.pos.models.Dispatch.DispatchRequest
import com.retailone.pos.models.Dispatch.DispatchResponse
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseCategory.ExpenseCategoryRes
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseHistory.ExpenseHistoryReq
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseHistory.ExpenseHistoryRes
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseImage.ExpenceImageRes
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseSubmit.ExpenseSubmitReq
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseSubmit.ExpenseSubmitRes
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseVendor.ExpenseVendorRes
import com.retailone.pos.models.GetCustomerModel.getCustomerReq
import com.retailone.pos.models.GetCustomerModel.getCustomerRes

import com.retailone.pos.models.GoodsToWarehouseModel.Stock.StockReturnRequests
import com.retailone.pos.models.GoodsToWarehouseModel.Stock.StockReturnResponses

import com.retailone.pos.models.GoodsToWarehouseModel.Stocklist.StockListResponse
import com.retailone.pos.models.LocalizationModel.LocalizationRes
import com.retailone.pos.models.LoginModels.LoginRequest
import com.retailone.pos.models.LoginModels.LoginResponse
import com.retailone.pos.models.LogoutModel.LogoutReq
import com.retailone.pos.models.LogoutModel.LogoutRes
import com.retailone.pos.models.MaterialRcvModel.MaterialRcvInv.MatRcvInvReq
import com.retailone.pos.models.MaterialRcvModel.MaterialRcvInv.MatRcvInvRes
import com.retailone.pos.models.OrganisationDetailsModel.OrganisationDetailsRes
import com.retailone.pos.models.PettycashReportModel.PettycashReportRes
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.PosAddToCartReq
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.PosAddToCartRes
import com.retailone.pos.models.PointofsaleModel.PosSaleModel.PosSaleReq
import com.retailone.pos.models.PointofsaleModel.SearchStoreProBarcodeModel.SearchStoreProBarcodeReq
import com.retailone.pos.models.PointofsaleModel.SearchStoreProBarcodeModel.SearchStoreProBarcodeRes
import com.retailone.pos.models.PointofsaleModel.SearchStroreProModel.SearchStoreProReq
import com.retailone.pos.models.PointofsaleModel.SearchStroreProModel.SearchStoreProRes
import com.retailone.pos.models.PosSalesDetailsModel.PosSalesDetails
import com.retailone.pos.models.ProductInventoryModel.InventoryUpdateReqModel.InventoryUpdateRequest
import com.retailone.pos.models.ProductInventoryModel.InventoryUpdateResModel.InventoryUpdateResponse
import com.retailone.pos.models.ProductInventoryModel.PiRequestModel.ProductInventoryRequest
import com.retailone.pos.models.ProductInventoryModel.PiResponseModel.ProductInventoryResponse
import com.retailone.pos.models.ReplaceModel.ReplaceSaleReq
import com.retailone.pos.models.ReplaceModel.ReturnSaleResRaw
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemReq
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemRes
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel.ReturnSaleReq
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel.SalesListRequest
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel.ReturnSaleRes
import com.retailone.pos.models.ReturnSalesItemModel.SalesReturnReasonModel.SalesReturnReasonRes
import com.retailone.pos.models.SalesListResponse
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.CancelSaleResponse
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.CancelSaleitemRequest
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.InvoiceReq
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.InvoiceRes
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.SalesDetailsReq
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.SalesDetailsRes
import com.retailone.pos.models.SalesPaymentModel.SalesList.SalesListReq
import com.retailone.pos.models.SalesPaymentModel.SalesList.SalesListRes
import com.retailone.pos.models.SendOTPModel.SendOtpRequest
import com.retailone.pos.models.SendOTPModel.SendOtpResponse
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.PastReqDetailsReq
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.PastReqDetailsRes
import com.retailone.pos.models.StockRequisitionModel.PastRequsitionModel.PastRequsitionReq
import com.retailone.pos.models.StockRequisitionModel.PastRequsitionModel.PastRequsitionRes
import com.retailone.pos.models.StockRequisitionModel.StockSearchModel.StockSearchReq
import com.retailone.pos.models.StockRequisitionModel.StockSearchModel.StockSearchRes
import com.retailone.pos.models.StockRequisitionModel.SubmitStockRequsitionModel.SubmitStockRequest
import com.retailone.pos.models.StockRequisitionModel.SubmitStockRequsitionModel.SubmitStockResponse
import com.retailone.pos.models.UserProfileModels.UserProfileResponse
import com.retailone.pos.models.VerifyOtpModel.VerifyOtpRequest
import com.retailone.pos.models.VerifyOtpModel.VerifyOtpResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("login")
    fun mposLogin(@Body loginRequest: LoginRequest): Call<LoginResponse>

    @GET("userprofile")
    fun userProfileAPI(): Call<UserProfileResponse>

    @POST("sendotp")
    fun sendOTP(@Body sendOtpRequest: SendOtpRequest): Call<SendOtpResponse>

    @POST("verifyotp")
    fun verifyOTP(@Body verifyOtpRequest: VerifyOtpRequest): Call<VerifyOtpResponse>

    @POST("changepin")
    fun changePIN(@Body changePinRequest: ChangePinRequest): Call<ChangePinResponse>

    @POST("searchproduct")
    fun stockReqSearch(@Body stockSearchReq: StockSearchReq): Call<StockSearchRes>

    @POST("searchproductbybarcode")
    fun stockReqSearchBarcode(@Body stockSearchBarcodeReq: StockSearchBarcodeReq): Call<StockSearchBarcodeRes>

    @POST("submitstockrequisition")
    fun submitStockRequsition(@Body subbmitStockRequest: SubmitStockRequest): Call<SubmitStockResponse>

    @POST("pastrequisition")
    fun pastRequsition(@Body pastRequsitionReq: PastRequsitionReq): Call<PastRequsitionRes>


    @POST("pastrequisitiondetails")
    fun pastRequsitionDetails(@Body pastReqDetailsReq: PastReqDetailsReq): Call<PastReqDetailsRes>

    @POST("receivematerialsnew")
    fun sendReceivedMaterials(@Body materialReceivedReq: MatRcvInvReq): Call<MatRcvInvRes>

    @POST("storestocks")
    fun getProductInventory(@Body productInventoryRequest: ProductInventoryRequest): Call<ProductInventoryResponse>


    @POST("updatestocks")
    fun updateInventoryStock(@Body inventoryUpdateRequest: InventoryUpdateRequest): Call<InventoryUpdateResponse>

    @POST("searchstoreproduct")
    fun searchStoreProduct(@Body searchStoreProReq: SearchStoreProReq): Call<SearchStoreProRes>

    @POST("searchstoreproductbybarcode")
    fun searchStoreProductBarcode(@Body searchStoreProBarcodeReq: SearchStoreProBarcodeReq): Call<SearchStoreProBarcodeRes>

    @POST("addtocart")
    fun addToCartPos(@Body posAddToCartReq: PosAddToCartReq): Call<PosAddToCartRes>

    @POST("sale")
  //  fun posSale(@Body posSaleReq: PosSaleReq): Call<PosSalesDetails>
    fun posSale(@Body body: com.google.gson.JsonObject): Call<PosSalesDetails>

    @GET("localizationdata")
    fun getLocalizationAPI(): Call<LocalizationRes>

    @GET("organisationdetails")
    fun getOrganisationDetailsAPI(): Call<OrganisationDetailsRes>

    @GET("expensecategories")
    fun getExpenceCategoryAPI(): Call<ExpenseCategoryRes>

    @GET("expensevendors")
    fun getExpenceVendorAPI(): Call<ExpenseVendorRes>

    @POST("expense")
    fun getExpenseSubmitAPI(@Body expenseSubmitReq: ExpenseSubmitReq): Call<ExpenseSubmitRes>

    @POST("pastexpenses")
    fun getExpenceHistoryAPI(@Body expenseHistoryReq: ExpenseHistoryReq): Call<ExpenseHistoryRes>

    @Multipart
    @POST("uploadimage")
    fun uploadInvoice(
      //@Part("phone") phone: RequestBody, // Change the annotation to @Part
        @Part file: MultipartBody.Part): Call<ExpenceImageRes>


    @Multipart
    @POST("uploadstnimage")
    fun uploadSTNImage(
        //@Part("phone") phone: RequestBody, // Change the annotation to @Part
        @Part file: MultipartBody.Part): Call<ExpenceImageRes>

    @POST("cashupdetails")
    fun getCashupDetailsAPI(@Body cashupDetailsReq: CashupDetailsReq): Call<CashupDetailsRes>

    @POST("sendotptocit")
    fun getSendOtpBankAPI(@Body sendOtpReq: SendOtpReq): Call<SendOtpRes>
    @POST("verifycitotp")
    fun getVerifyOtpBankAPI(@Body verifyOtpReq: VerifyOtpReq): Call<VerifyOtpRes>

    @POST("submitcashup")
    fun getCashupSubmitAPI(@Body cashupSubmitReq: CashupSubmitReq): Call<CashupSubmitRes>

    @POST("saleslist")
    fun getSalesListAPI(@Body salesListRes: SalesListReq): Call<SalesListRes>

    @POST("saledetails")
    fun getSalesDetailsAPI(@Body saleDetailsReq: SalesDetailsReq): Call<SalesDetailsRes>

    @POST("getsaledetails")
    fun getReturnSalesItemAPI(@Body returnItemReq: ReturnItemReq): Call<ReturnItemRes>

  //  @POST("replacesale/replace")
   // fun replaceSale(@Body replaceSaleReq: ReplaceSaleReq): Call<ReturnSaleRes>

    // ApiService.kt
    @POST("replacesale/replace")
    fun replaceSale(@Body req: ReplaceSaleReq): Call<okhttp3.ResponseBody>

    @POST("returnsale")
    fun getReturnSalesSubmitAPI(@Body returnSaleReq: ReturnSaleReq): Call<ReturnSaleRes>

    @GET("salesreturnreasons")
    fun getReturnReasonAPI(): Call<SalesReturnReasonRes>


    @POST("allinvocesandpaymnent")
    fun getInvoiceAPI(@Body invoiceReq: InvoiceReq): Call<InvoiceRes>

    @POST("logout")
    fun getLogoutAPI(@Body logoutReq: LogoutReq): Call<LogoutRes>

    @POST("getmonthlyattendance")
    fun getMonthlyAttendanceAPI(@Body monthlyAttendanceReq: MonthlyAttendanceReq): Call<MonthlyAttendanceRes>

    @POST("getcustomer")
    fun getCustomerInfoAPI(@Body getCustomerReq: getCustomerReq): Call<getCustomerRes>

    @POST("addcustomer")
    fun addNewCustAPI(@Body addNewCustReq: AddNewCustReq): Call<AddNewCustRes>

    @GET("pettycashreport")
    fun getPettyCashReport(): Call<PettycashReportRes>

    @POST("storestocks")
    fun getStockListAPI(@Body request: Map<String, Int>): Call<StockListResponse>

    @GET("stock-return")
    fun getStockReturns(): Call<StockReturnResponse>

    @POST("stock-return/dispatch")
    fun dispatchStock(@Body request: DispatchRequest): Call<DispatchResponse>

    @POST("stock-return/store")
    fun submitStockReturn(@Body request: StockReturnRequests): Call<StockReturnResponses>

    @POST("cancel_invoice")
    fun cancelItemAPI(@Body request: CancelSaleitemRequest): Call<CancelSaleResponse>

  /*  @POST("saleslist")
    fun getSalesList(@Query("days") days: Int): Call<SalesListResponse>*/

    @POST("saleslist")
    fun getSalesList(
        @Query("days") days: Int,
        @Body request: SalesListRequest
    ): Call<SalesListResponse>

    @POST("replacesale/saleslist")
    fun getReplaceSalesList(
        @Query("days") days: Int,
        @Body request: SalesListRequest
    ): Call<SalesListResponse>


//    @Headers(
//    "Accept: application/json",
//    "Content-Type: application/json",
//    "Platform: android")
//    @GET("pettycash?store_id={storeid}")
//    fun getPettyCashData(@Path("storeid") store_id:String): Call<PettycashReportRes>

/*    @GET("pettycash")
    fun getPettyCashData(@Query("store_id") storeId:String): Call<PettycashReportRes>*/
    //Query Parameter


    @GET("pettycash/{store_id}")
    fun getPettyCashData(@Path("store_id") storeId: String): Call<PettycashReportRes>
    //Path Parameter




}