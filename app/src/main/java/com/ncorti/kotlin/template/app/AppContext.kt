package com.ncorti.kotlin.template.app

import android.app.Application
import com.ncorti.kotlin.template.app.utils.appContext

class AppContext : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this
    }
}
