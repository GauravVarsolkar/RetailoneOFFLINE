package com.retailone.pos

import android.app.Application
import android.util.Log
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.utils.FeatureManager
import com.retailone.pos.workers.SyncWorker
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.d("MyApplication", "========================================")
        Log.d("MyApplication", "APP STARTED - onCreate() called")
        Log.d("MyApplication", "========================================")

        // Initialize WorkManager & schedule sync
        try {
            val workManager = WorkManager.getInstance(this)
            Log.d("MyApplication", "✅ WorkManager initialized successfully")

            SyncWorker.scheduleSync(this)
            Log.d("MyApplication", "✅ Sync scheduled (instant + every 15 min)")

        } catch (e: Exception) {
            Log.e("MyApplication", "❌ WorkManager Error: ${e.message}")
            e.printStackTrace()
        }

        // Initialize FeatureManager with modules from LoginSession
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val modules = LoginSession.getInstance(this@MyApplication).getModules().first()
                FeatureManager.init(modules)
                Log.d("MyApplication", "✅ FeatureManager initialized successfully")
            } catch (e: Exception) {
                Log.e("MyApplication", "❌ FeatureManager Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}