package com.retailone.pos.models.PosSalesDetailsModel

// Existing Receipt Type models
data class ReceiptType(
    val id: Int,
    val code: String,
    val name: String,
    val useYn: String?,
    val created_at: String?,
    val updated_at: String?
)

data class ReceiptTypeResponse(
    val status: Int,
    val message: String,
    val data: List<ReceiptType>
)

// Copy Receipt Request model
data class CopyReceiptReq(
    val sales_id: String,
    val store_id: String,
    val type: String = "Refund"
)

// Copy Receipt Response models
data class CopyReceiptRes(
    val status: Int,
    val message: String?,
    val data: Data?
) {
    data class Data(
        val returned_invoice_id: String?,
        val invoice_id: String?,
        val tpin_no: String?,
        val pruchase_date_time: String?,
        val subtal: String?,
        val tax_ex: String?,
        val tax: String?,
        val tax_amount: String?,
        val grand_total: String?,
        val payment_type: String?,
        val rcptType: String?,
        val customer_name: String?,
        val buyers_tpin: String?,
        val customer_mob_no: String?,
        val returned_date: String?,
        val store: Store?,
        val vsdc_reciept: List<VsdcReceipt>?,
        val returned_items: List<ReturnedItem>?,
        val tax_summery: List<TaxSummery>?,
        val SalesDetailsResponse: SalesDetailsResponse?
    )

    data class Store(
        val id: Int?,
        val store_name: String?,
        val address: String?,
        val vat_no: String?,
        val phone_no: String?,
        val logo: String?
    )

    data class VsdcReceipt(
        val rcptNo: Int?,
        val intrlData: String?,
        val rcptSign: String?,
        val totRcptNo: Int?,
        val vsdcRcptPbctDate: String?,
        val sdcId: String?,
        val mrcNo: String?,
        val qrCodeUrl: String?,
        val store_id: Int?,
        val sales_id: Int?,
        val sales_type: String?,
        val receipt_type: String?,
        val created_at: String?,
    )

    data class ReturnedItem(
        val id: Int?,
        val tax: Double?,
        val uom: String?,
        val batch: String?,
        val status: Int?,
        val discount: Double?,
        val quantity: Double?,
        val sales_id: String?,
        val sub_total: Double?,
        val product_id: Int?,
        val tax_amount: Double?,
        val product_name: String?,
        val retail_price: Double?,
        val total_amount: Double?,
        val discount_rate: Double?,
        val discounted_total: Double?,
        val return_quantity: Int?,
        val tax_inclusive_price: Double?,
        val distribution_pack_id: Int?,
        val distribution_pack_name: String?,
        val tax_details: ItemTax?,           // ✅ ADDED
        val created_at: String?,
        val updated_at: String?
    )

    data class ItemTax(                      // ✅ ADDED
        val id: Int?,
        val name: String?,
        val amount: Int?,
        val type: Int?,
        val organization_id: Int?,
        val deleted_at: String?,
        val status: Int?,
        val created_at: String?,
        val updated_at: String?,
        val code: String?
    )

    data class TaxSummery(
        val code: String?,
        val rate: Double?,
        val code_name: String?,
        val tax_amount: Double?,
        val gross_total: Double?,
        val taxable_value: Double?
    )
}

// Sales Details Request
data class SalesDetailsReq(
    val sale_id: String
)

// Sales Details Response
data class SalesDetailsResponse(
    val status: Int,
    val message: String?,
    val data: List<SalesData>?
) {
    data class SalesData(
        val sale_id: Int?,
        val invoice_id: String?,
        val store_id: Int?,
        val customer_id: Int?,
        val payment_type: String?,
        val sub_total: Double?,
        val tax_amount: Double?,
        val grand_total: Double?,
        val created_at: String?,
        val store_details: StoreInfo?,
        val customer: CustomerInfo?,
        val salesItem: List<SalesItemInfo>?,
        val vsdc_reciept: List<VsdcInfo>?,
        val summary: Summary?
    )

    data class StoreInfo(
        val store_name: String?,
        val address: String?
    )

    data class CustomerInfo(
        val customer_name: String?,
        val buyers_tpin: String?,
        val customer_mob_no: String?
    )

    data class SalesItemInfo(
        val product_name: String?,
        val quantity: String?,
        val uom: String?,
        val tax_inclusive_price: String?,
        val total_amount: Double?
    )

    data class VsdcInfo(
        val sdcId: String?,
        val mrcNo: String?,
        val rcptSign: String?,
        val intrlData: String?,
        val qrCodeUrl: String?
    )

    data class Summary(
        val total_sub_total: Double?,
        val total_tax_amount: Double?
    )
}

// ── Sale Receipt Response ─────────────────────────────────────────────────────

data class SaleReceiptRes(
    val status: Int?,
    val message: String?,
    val data: Data?
) {
    data class Data(
        val store: Store?,
        val store_manager_id: String?,
        val payment_type: String?,
        val invoice_id: String?,
        val sub_total: String?,
        val tax: String?,
        val tax_amount: String?,
        val discount_amount: String?,
        val subtotal_after_discount: String?,
        val grand_total: String?,
        val amount_tendered: String?,
        val salesItem: List<SalesItem>?,
        val tax_summery: List<TaxSummery>?,   // typo matches backend exactly
        val purchase_date_time: String?,
        val customer_name: String?,
        val customer_mob_no: String?,
        val vat_no: String?,
        val tpin_no: String?,
        val buyers_tpin: String?,
        val buyers_vat_no: String?,
        val tax_ex: String?,
        val ej_no: String?,
        val ej_activation_date: String?,
        val sdc_id: String?,
        val receipt_no: String?,
        val internal_data: String?,
        val receipt_sign: String?,
        val warning: String?,
        val rcptType: String?,
        val vsdc_reciept: List<VsdcReceipt>?        // updated: proper object instead of String
    )

    data class Store(
        val id: Int?,
        val store_name: String?,
        val station_code: String?,
        val address: String?,
        val organization_id: Int?,
        val ho_manager_id: Int?,
        val cluster_id: Int?,
        val phone_no: String?,
        val logo: String?,
        val logo_image_name: String?,
        val latitude: String?,
        val longitude: String?,
        val induction_date: String?,
        val location: String?,
        val internal_data: String?,
        val petty_cash_opening_balance: String?,
        val deleted_at: String?,
        val created_at: String?,
        val updated_at: String?,
        val status: Int?,
        val exposure_limit: String?,
        val branch_code: String?,
        val device_serial_no: String?
    )

    data class SalesItem(
        val batchno: String?,
        val quantity: Double?,
        val tax_inclusive_price: Double?,
        val retail_price: Double?,
        val distribution_pack_id: String?,
        val distribution_pack_name: String?,
        val product_id: String?,
        val product_name: String?,
        val total_amount: Double?,
        val discounted_total: Double?,        // updated: was total_after_discount
        val uom: String?,
        val whole_sale_price: String?,
        val tax_details: ItemTax?,            // updated: was tax
        val discount_rate: Double?
    )

    data class ItemTax(
        val id: Int?,
        val name: String?,
        val amount: String?,
        val type: String?,
        val organization_id: Int?,
        val deleted_at: String?,
        val status: Int?,
        val created_at: String?,
        val updated_at: String?,
        val code: String?
    )

    data class TaxSummery(                   // typo matches backend exactly
        val code: String?,
        val rate: Double?,
        val taxable_value: Double?,
        val tax_amount: Double?,
        val gross_total: Double?,
        val code_name: String?
    )

    data class VsdcReceipt(                  // new class for vsdc_reciept object
        val mrcNo: String?,
        val sdcId: String?,
        val rcptNo: Int?,
        val totRcptNo: Int?,
        val rcptSign: String?,
        val intrlData: String?,
        val qrCodeUrl: String?,
        val sales_type: String?,
        val receipt_type: String?,
        val vsdcRcptPbctDate: String?,
        val organization_id: Int?,
        val store_id: Int?,
        val sales_id: Int?,
        val created_at: String?
    )
}