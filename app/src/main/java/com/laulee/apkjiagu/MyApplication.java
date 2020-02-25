package com.laulee.apkjiagu;

import android.app.Application;
import android.content.Context;
import android.util.Log;

/**
 * Created by laulee on 2020-02-20.
 */
public class MyApplication extends Application {

    private static final String TAG = "MyApplication";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
    }
}
