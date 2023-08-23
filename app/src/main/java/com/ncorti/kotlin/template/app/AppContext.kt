package com.ncorti.kotlin.template.app

import android.app.Application
import android.content.Context
import com.ncorti.kotlin.template.app.plugin.MyInstrumentation
import com.ncorti.kotlin.template.app.utils.appContext
import com.ncorti.kotlin.template.app.utils.baseInstrumentation
import com.ncorti.kotlin.template.app.utils.hookInstrumentation
import me.weishu.reflection.Reflection

class AppContext : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this
        hookInstrumentation(MyInstrumentation(baseInstrumentation))
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Reflection.unseal(base)
    }
}
