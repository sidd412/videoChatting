package com.videoChatting.echat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
