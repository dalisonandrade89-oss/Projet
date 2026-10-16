package com.projet.app

import android.app.Application
import com.projet.app.di.appModule
import com.projet.app.work.SyncScheduler
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class ProjetApp : Application() {

    private val syncScheduler: SyncScheduler by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@ProjetApp)
            modules(appModule)
        }
        syncScheduler.schedulePeriodic()
    }
}
