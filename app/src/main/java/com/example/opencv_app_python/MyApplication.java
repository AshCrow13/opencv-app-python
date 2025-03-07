package com.example.opencv_app_python;

import android.app.Application;
import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.Python;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Inicializar Python
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }
}