package com.retailone.pos.models.ReplaceModel


import com.google.gson.Gson
import com.google.gson.JsonElement
import com.retailone.pos.models.PosSalesDetailsModel.VsdcReceipt
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel.Data
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel.ReturnSaleRes
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel.Store




data class ReturnSaleResRaw(
    val status: Int,
    val message: String,
    val data: JsonElement?
)

object ReturnSaleResMapper {

    fun toReturnSaleRes(raw: ReturnSaleResRaw, gson: Gson): ReturnSaleRes {
        val typedData: Data = when {
            raw.data == null || raw.data.isJsonNull -> emptyData()
            raw.data.isJsonObject -> gson.fromJson(raw.data, Data::class.java)
            raw.data.isJsonArray -> emptyData() // backend sent [], avoid crash
            else -> emptyData()
        }
        return ReturnSaleRes(
            status = raw.status,
            message = raw.message,
            data = typedData
        )
    }

    private fun emptyData(): Data = Data(
        returned_items = emptyList(),
        total = 0.0,
        returned_invoice_id = "",
        grand_total = 0.0,

        tax = null,
        tax_amount = 0.0,
        tax_ex = null,

        store = Store(
            id = 0,
            store_name = "",
            station_code = "",
            address = "",
            organization_id = 0,
            ho_manager_id = 0,
            cluster_id = 0,
            phone_no = "",
            logo = null,
            logo_image_name = null,
            latitude = "",
            longitude = "",
            induction_date = "",
            location = "",
            internal_data = null,
            petty_cash_opening_balance = null,
            deleted_at = null,
            created_at = "",
            updated_at = "",
            status = 0,
            exposure_limit = null,
            store_incharge = null
        ),

        vat_no = null,
        tpin_no = null,
        buyers_tpin = null,
        ej_no = null,
        ej_activation_date = null,
        sdc_id = null,
        receipt_no = null,
        internal_data = null,
        receipt_sign = null,
        returned_date = null, // <- keep nullable; API often omits it
        customer_name = null,
        customer_mob_no = null,
        rcptType = "",
        subtal = "",
        ogRcpt_no = 0.0,
        sub_total = 0.0,
        tax_summery = null,
        vsdc_reciept = null,
    )
}