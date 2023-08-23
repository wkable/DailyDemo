package com.ncorti.kotlin.template.app.plugin;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.ncorti.kotlin.template.app.utils.CommonUtilsKt;
import com.ncorti.kotlin.template.app.utils.PluginUtilsKt;

import java.io.File;

public class MyInstrumentation extends Instrumentation {

    private static final String TAG = "MyInstrumentation";
    private final Instrumentation mBase;
    private final File pluginFile = new File(Environment.getExternalStorageDirectory(), "test.aar");

    private final ClassLoader newClassLoader;

    public MyInstrumentation(Instrumentation mBase) {
        this.mBase = mBase;
        Context appContext = CommonUtilsKt.appContext;
        PluginUtilsKt.newClassLoader(pluginFile, appContext);
        newClassLoader = appContext.getClassLoader();
        Log.i(TAG, "MyInstrumentation: " + newClassLoader);
    }

    public Activity newActivity(Class<?> clazz, Context context,
                                IBinder token, Application application, Intent intent, ActivityInfo info,
                                CharSequence title, Activity parent, String id,
                                Object lastNonConfigurationInstance) throws InstantiationException,
            IllegalAccessException {
        Log.i(TAG, "newActivity1: " + clazz);
        return mBase.newActivity(clazz, context, token, application, intent, info, title, parent, id, lastNonConfigurationInstance);
    }

    public Activity newActivity(ClassLoader cl, String className,
                                Intent intent)
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        Log.i(TAG, "newActivity2: " + className);
        return mBase.newActivity(newClassLoader, className, intent);
    }
}
