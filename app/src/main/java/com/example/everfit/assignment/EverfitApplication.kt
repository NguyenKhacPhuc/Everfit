package com.example.everfit.assignment

import android.app.Application
import com.example.everfit.assignment.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class EverfitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@EverfitApplication)
            modules(appModule)
        }
    }
}
