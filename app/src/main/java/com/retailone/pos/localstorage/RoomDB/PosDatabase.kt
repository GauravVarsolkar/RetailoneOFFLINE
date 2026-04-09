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
        SalesDetailsEntity::class,           // ✅ ADDED for sales details offline support
        PendingCancelSaleEntity::class,      // ✅ ADDED for cancel sale offline support
        PendingReplaceEntity::class,         // ✅ ADDED for replace sale offline support
        PendingGoodsReturnEntity::class,     // ✅ ADDED for warehouse return offline support
        StockListEntity::class,              // ✅ ADDED for warehouse stock list offline support
        StockReturnEntity::class,             // ✅ ADDED for stock return list offline support
        UserEntity::class,                    // ✅ ADDED for offline login support
        PendingExpenseEntity::class,           // ✅ ADDED for offline expense support
        CustomerDiscountEntity::class,         // ✅ ADDED for offline customer discounts
        ReceiptTypeEntity::class,               // ✅ ADDED for offline receipt types support
        StoreSettingsEntity::class,              // ✅ ADDED for store-specific settings
        PendingDispatchEntity::class            // ✅ ADDED for stock return dispatch offline support
    ],
    version = 22,                          // ✅ INCREMENTED from 21 to 22
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
    abstract fun pendingCancelSaleDao(): PendingCancelSaleDao // ✅ ADDED for cancel sale offline support
    abstract fun pendingReplaceDao(): PendingReplaceDao      // ✅ ADDED for replace sale offline support
    abstract fun pendingGoodsReturnDao(): PendingGoodsReturnDao // ✅ ADDED for warehouse return offline support
    abstract fun stockListDao(): StockListDao                    // ✅ ADDED for warehouse stock list offline support
    abstract fun stockReturnDao(): StockReturnDao              // ✅ ADDED for stock return list offline support
    abstract fun userDao(): UserDao                            // ✅ ADDED for offline login support
    abstract fun pendingExpenseDao(): PendingExpenseDao        // ✅ ADDED for expense offline support
    abstract fun customerDiscountDao(): CustomerDiscountDao    // ✅ ADDED for offline customer discounts
    abstract fun receiptTypeDao(): ReceiptTypeDao              // ✅ ADDED for offline receipt types
    abstract fun storeSettingsDao(): StoreSettingsDao         // ✅ ADDED for store settings
    abstract fun pendingDispatchDao(): PendingDispatchDao     // ✅ ADDED for stock return dispatch

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


