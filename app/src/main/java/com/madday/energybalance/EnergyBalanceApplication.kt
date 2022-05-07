package com.madday.energybalance

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class EnergyBalanceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // start Koin!
        startKoin {
            // declare used Android context
            androidContext(this@EnergyBalanceApplication)
            // declare modules
            modules(mainModule)
        }
    }
}