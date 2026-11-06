package com.arohi.healthcare.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.arohi.healthcare.databinding.ActivitySplashBinding;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.navigation.AuthNavigationRouter;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.AuthRepository;
import com.arohi.healthcare.services.FirebaseProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * 1. Splash Screen
 * Initializes AROHI session check, synchronizes Firebase Auth state if connected,
 * and routes the user without unnecessary re-logins.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1100L;
    private ActivitySplashBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);

        new Handler(Looper.getMainLooper()).postDelayed(this::resolveSessionAndNavigate, SPLASH_DELAY_MS);
    }

    private void resolveSessionAndNavigate() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        FirebaseAuth auth = FirebaseProvider.getAuth(this);
        if (auth != null && auth.getCurrentUser() != null) {
            FirebaseUser firebaseUser = auth.getCurrentUser();
            authRepository.fetchUserAccountFromFirestore(firebaseUser, new AuthCallback<UserAccount>() {
                @Override
                public void onSuccess(UserAccount result) {
                    AuthNavigationRouter.routeAuthenticatedUser(SplashActivity.this, result);
                }

                @Override
                public void onError(String errorMessage) {
                    AuthNavigationRouter.routeFromSplash(SplashActivity.this, authRepository.getSessionManager());
                }
            });
        } else {
            AuthNavigationRouter.routeFromSplash(this, authRepository.getSessionManager());
        }
    }
}
