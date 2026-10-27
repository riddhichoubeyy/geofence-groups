package com.example.geofencegroups

import android.app.Application
import com.example.geofencegroups.push.NotificationHelper

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)
    }
}
