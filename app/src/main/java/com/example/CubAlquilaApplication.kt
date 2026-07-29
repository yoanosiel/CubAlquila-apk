package com.example

import android.app.Application
import com.parse.Parse

class CubAlquilaApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val appId = "M93BK0NLWqLy32CvT5fwVceB12mLYurgrh5L2VKb"
        val clientKey = "N0XwhiJ6a0HFibrtWEQOtrTopbCbE0UwZPFpHBZB"

        Parse.initialize(
            Parse.Configuration.Builder(this)
                .applicationId(appId)
                .clientKey(clientKey)
                .server("https://parseapi.back4app.com")
                .build()
        )
    }
}
