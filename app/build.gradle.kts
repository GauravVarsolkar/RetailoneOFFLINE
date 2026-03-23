plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp") version "1.9.22-1.0.16"

}

android {
    namespace = "com.retailone.pos"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.retailone.pos"
        minSdk = 25
        targetSdk = 33
        versionCode = 1
        versionName = "3.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        //shrinkResources
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true

       // buildConfig = true

    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    /*
        ext {
             coroutinesVersion = "1.7.0-RC"
        }*/


    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.media3:media3-common-ktx:1.8.0")


    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    //ViewModel and livedata
    implementation( "androidx.lifecycle:lifecycle-extensions:2.2.0")

    // Activity KTX for viewModels()
    implementation ("androidx.activity:activity-ktx:1.8.0")

    // Coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

    // Coroutine Lifecycle Scopes
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")


    //data-store
    implementation ("androidx.datastore:datastore-preferences:1.1.0-alpha06")
    implementation ("androidx.datastore:datastore-preferences-core:1.1.0-alpha06")


    //Retrofit
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    //implementation ("com.squareup.retrofit2:converter-moshi:2.4.0")

    implementation ("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    //Glide
    implementation ("com.github.bumptech.glide:glide:4.14.2")
    annotationProcessor("com.github.bumptech.glide:compiler:4.14.2")

    //implementation (files("libs/core-3.1.0.jar"))

    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation ("com.google.zxing:core:3.4.1")

    // ROOM (KSP-based)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    //qrcode
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

//    Simple Flow - How It Will Work 📱
//Scenario 1: Online Sale → Return
//text
//1. User makes a sale in ONLINE mode
//   ↓
//2. API responds with success + sale_id
//   ↓
//3. App saves sale to LOCAL DATABASE (Room)
//   - Sale details (customer, date, amount)
//   - All items with batches
//   - Timestamp = NOW
//   ↓
//4. User clicks "Return" immediately or later
//   ↓
//5. Return screen searches LOCAL DATABASE by sale_id
//   ↓
//6. Sale found! → Shows items for return
//   ↓
//7. After 7 days, automatic cleanup deletes this sale
//Scenario 2: Offline Sale → Return
//text
//1. User makes a sale in OFFLINE mode (no internet)
//   ↓
//2. App generates temporary sale_id (e.g., "OFF_12345")
//   ↓
//3. App saves sale to LOCAL DATABASE
//   - Sale details
//   - All items with batches
//   - Mark as "pending sync"
//   - Timestamp = NOW
//   ↓
//4. User clicks "Return" (still offline)
//   ↓
//5. Return screen searches LOCAL DATABASE
//   ↓
//6. Sale found! → Shows items for return
//   ↓
//7. Later when internet comes back, sync to server
//   ↓
//8. After 7 days, automatic cleanup deletes this sale
//Scenario 3: Return Screen Search
//text
//User enters Invoice ID in Return screen
//   ↓
//App searches LOCAL DATABASE
//   ↓
//   ├─→ FOUND: Display items with batches
//   │   (works offline too!)
//   │
//   └─→ NOT FOUND: Show "Sale not found or expired"
//       (either doesn't exist or older than 7 days)
//Scenario 4: Automatic Cleanup
//text
//Day 1: Sale created → Saved in database (timestamp = Jan 1)
//Day 2-7: Sale visible in Return screen
//Day 8: App opens
//   ↓
//App checks database on startup
//   ↓
//Finds sales older than 7 days
//   ↓
//Deletes them automatically
//   ↓
//User tries to return Day 1 sale → "Not found or expired"
//Key Benefits:
//✅ No API calls for searching returns (faster!)
//✅ Works offline completely
//✅ Both online & offline sales stored the same way
//✅ Auto-cleanup keeps database small
//✅ 7-day window for returns (configurable)
//
//Simple Data Flow:
//text
//POS Sale Success
//    ↓
//Save to Room Database
//    ↓
//┌─────────────────────────────┐
//│  Local Database (SQLite)    │
//│  ├─ sales table             │
//│  └─ sale_items table        │
//└─────────────────────────────┘
//    ↓
//Return Screen reads from here
//    ↓
//Shows items for return/replace
//Does this flow make sense? Should I proceed with the implementation? 🚀
//
//Prepared using Claude Sonnet 4.5

}