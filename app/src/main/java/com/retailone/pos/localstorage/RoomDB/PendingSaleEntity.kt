package com.retailone.pos.localstorage.RoomDB

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "pending_sales")
@TypeConverters(Converters::class)
data class PendingSaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Sale data (stored as JSON strings for complex objects)
    val customer_mob_no: String,
    val customer_name: String,
    val customer_id: Int,
    val discount_amount: String,
    val grand_total: String,
    val payment_type: String,
    val sales_items_json: String, // List<PosSalesItem> as JSON
    val sub_total: String,
    val subtotal_after_discount: String,
    val tax: String,
    val tax_amount: String,
    val store_id: String,
    val store_manager_id: String,
    val amount_tendered: String,
    val sale_date_time: String,
    val tin_tpin_no: String,
    val invoice_id: String,

    // Sync status tracking
    val sync_status: String = "PENDING", // PENDING, SYNCING, SYNCED, FAILED
    val sync_attempts: Int = 0,
    val last_sync_attempt: Long? = null,
    val error_message: String? = null,

    // Timestamps
    val created_at: Long = System.currentTimeMillis(),
    val synced_at: Long? = null
)
