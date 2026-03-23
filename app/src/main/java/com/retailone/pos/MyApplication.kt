package com.retailone.pos

import android.app.Application
import android.util.Log
import com.retailone.pos.workers.SyncWorker
import androidx.work.WorkManager
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.d("MyApplication", "========================================")
        Log.d("MyApplication", "APP STARTED - onCreate() called")
        Log.d("MyApplication", "========================================")

        try {
            val workManager = WorkManager.getInstance(this)
            Log.d("MyApplication", "✅ WorkManager initialized successfully")

            // ⚡ This will now sync immediately + every 15 min
            SyncWorker.scheduleSync(this)
            Log.d("MyApplication", "✅ Sync scheduled (instant + every 15 min)")

        } catch (e: Exception) {
            Log.e("MyApplication", "❌ Error: ${e.message}")
            e.printStackTrace()
        }
    }
}
