package com.arohi.healthcare.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

/**
 * Locale management utility for seamless English and Bangla (বাংলা) localization.
 * Compatible with Android 8.0+ (API 26+) and integrates with AppCompatDelegate.
 */
public final class LocaleHelper {

    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_BANGLA = "bn";

    private LocaleHelper() {
        // Prevent instantiation
    }

    /**
     * Applies the selected language across the entire application without losing session state.
     */
    public static void setLocale(Context context, String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            languageCode = LANGUAGE_ENGLISH;
        }

        // 1. Modern AppCompat application-level locale switching
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageCode);
        AppCompatDelegate.setApplicationLocales(appLocale);

        // 2. Direct resources configuration update for immediate rendering
        updateResources(context, languageCode);
        Context appContext = context.getApplicationContext();
        if (appContext != null && appContext != context) {
            updateResources(appContext, languageCode);
        }
    }

    /**
     * Wraps context with the appropriate Locale configuration.
     */
    public static Context wrapContext(Context context, String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            languageCode = LANGUAGE_ENGLISH;
        }

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(new LocaleList(locale));
            return context.createConfigurationContext(configuration);
        } else {
            configuration.setLocale(locale);
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }

    private static void updateResources(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    public static boolean isBangla(Context context) {
        SessionManager sm = new SessionManager(context);
        return LANGUAGE_BANGLA.equalsIgnoreCase(sm.getAppLanguage());
    }
}
