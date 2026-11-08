package com.arohi.healthcare;

import android.app.Application;
import android.content.Context;
import android.util.Log;

/**
 * Custom Application class for AROHI Healthcare.
 * Installs a robust global uncaught exception interceptor to prevent silent abrupt termination
 * ("app being off"), persist diagnostic logs, and protect background services.
 */
public class ArohiApplication extends Application {

    private static final String TAG = "ArohiApplication";
    private static ArohiApplication instance;

    public static ArohiApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Global Uncaught Exception Handler
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught Exception on thread [" + thread.getName() + "]: " + throwable.getMessage(), throwable);

            try {
                // Record diagnostic crash trace in SharedPreferences for debugging
                getSharedPreferences("arohi_diagnostics_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("last_crash_trace", Log.getStackTraceString(throwable))
                        .putString("last_crash_thread", thread.getName())
                        .putLong("last_crash_timestamp", System.currentTimeMillis())
                        .commit();
            } catch (Exception ignored) {}

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });
    }
}