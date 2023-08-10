package com.ncorti.kotlin.template.app

import android.app.Application
import android.content.Context
import com.ncorti.kotlin.template.app.utils.appContext
import me.weishu.reflection.Reflection

class AppContext : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Reflection.unseal(base);
    }
}
