package com.example.runningapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

//use HiltAndroidApp to tell the application that we need to inject dependencies using dagger hilt
//when we set things in the BaseApplication we can use them through out the application lifetime
@HiltAndroidApp
class BaseApplication :Application() {

    override fun onCreate() {
        super.onCreate()

        //Timber for logging
        Timber.plant(Timber.DebugTree())
    }

}