package com.arohi.healthcare;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.arohi.healthcare.utils.SessionManager;

/**
 * Custom Application class for AROHI Healthcare.
 * Installs a robust global uncaught exception interceptor to prevent silent abrupt termination
 * ("app being off"), persist diagnostic logs, and protect background services.
 * Automatically restores user's persisted Appearance (Light/Dark/System) and Language (English/Bangla).
 */
public class ArohiApplication extends Application {

    private static final String TAG = "ArohiApplication";
    private static ArohiApplication instance;

    public static ArohiApplication getInstance() {
        return instance;
    }

    @Override
    protected void attachBaseContext(Context base) {
        try {
            SessionManager sm = new SessionManager(base);
            super.attachBaseContext(com.arohi.healthcare.utils.LocaleHelper.wrapContext(base, sm.getAppLanguage()));
        } catch (Exception e) {
            super.attachBaseContext(base);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Restore saved Theme & Language preferences (Phase 10)
        try {
            SessionManager sessionManager = new SessionManager(this);
            sessionManager.applySavedTheme();
            sessionManager.applySavedLocale(this);
        } catch (Exception e) {
            Log.w(TAG, "Failed initializing theme or locale on application start: " + e.getMessage());
        }

        // Initialize OpenStreetMap (OSMDroid) with compliant User-Agent & private cache path
        try {
            org.osmdroid.config.Configuration.getInstance().load(
                    this, getSharedPreferences("arohi_osmdroid_prefs", Context.MODE_PRIVATE));
            org.osmdroid.config.Configuration.getInstance().setUserAgentValue(getPackageName());
            org.osmdroid.config.Configuration.getInstance().setOsmdroidBasePath(getCacheDir());
            org.osmdroid.config.Configuration.getInstance().setOsmdroidTileCache(
                    new java.io.File(getCacheDir(), "osmdroid_tiles"));
        } catch (Exception e) {
            Log.w(TAG, "OSMDroid config init skipped: " + e.getMessage());
        }

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