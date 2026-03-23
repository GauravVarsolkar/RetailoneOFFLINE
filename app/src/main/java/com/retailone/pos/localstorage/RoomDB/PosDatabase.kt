package com.retailone.pos.localstorage.RoomDB

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        StoreProductEntity::class,
        ProductInventoryEntity::class,
        PendingSaleEntity::class,
        CompletedSaleEntity::class,
        DetailedSaleEntity::class,
        ReturnReasonEntity::class,
        PendingReturnEntity::class,
        PaymentInvoiceEntity::class,         // ✅ ADDED for sales/payment offline support
        SalesDetailsEntity::class            // ✅ ADDED for sales details offline support
    ],
    version = 9,                           // ✅ INCREMENTED from 8 to 9
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PosDatabase : RoomDatabase() {

    abstract fun storeProductDao(): StoreProductDao
    abstract fun pendingSaleDao(): PendingSaleDao
    abstract fun productInventoryDao(): ProductInventoryDao
    abstract fun completedSaleDao(): CompletedSaleDao
    abstract fun detailedSaleDao(): DetailedSaleDao
    abstract fun returnReasonDao(): ReturnReasonDao
    abstract fun pendingReturnDao(): PendingReturnDao
    abstract fun paymentInvoiceDao(): PaymentInvoiceDao  // ✅ ADDED for sales/payment offline support
    abstract fun salesDetailsDao(): SalesDetailsDao       // ✅ ADDED for sales details offline support

    companion object {
        @Volatile
        private var INSTANCE: PosDatabase? = null

        fun getDatabase(context: Context): PosDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PosDatabase::class.java,
                    "pos_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


