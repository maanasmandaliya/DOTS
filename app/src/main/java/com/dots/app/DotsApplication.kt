package com.dots.app

import android.app.Application
import android.content.Context

class DotsApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scheduler.schedule()
    }
}

val Context.container: AppContainer get() = (applicationContext as DotsApplication).container
