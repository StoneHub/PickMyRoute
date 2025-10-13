package com.stonecode.pickmyroute

import android.app.Application
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PickMyRouteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase is auto-initialized, but we can configure it here if needed
        Firebase.analytics.setAnalyticsCollectionEnabled(true)
    }
}
