package com.arohi.healthcare.activities;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.arohi.healthcare.utils.LocaleHelper;
import com.arohi.healthcare.utils.SessionManager;

/**
 * Base Activity for all AROHI screens.
 * Ensures saved locale (English / বাংলা) and dark/light theme apply consistently
 * across every Activity, Dialog, Fragment, and backstack screen.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private String appliedLanguage;

    @Override
    protected void attachBaseContext(Context newBase) {
        SessionManager sm = new SessionManager(newBase);
        String lang = sm.getAppLanguage();
        super.attachBaseContext(LocaleHelper.wrapContext(newBase, lang));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SessionManager sm = new SessionManager(this);
        appliedLanguage = sm.getAppLanguage();
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SessionManager sm = new SessionManager(this);
        String latestLanguage = sm.getAppLanguage();
        if (appliedLanguage != null && !appliedLanguage.equalsIgnoreCase(latestLanguage)) {
            recreate();
        }
    }
}
