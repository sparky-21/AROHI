package com.arohi.healthcare.navigation;

import android.app.Activity;
import android.content.Intent;
import com.arohi.healthcare.activities.BasicProfileSetupActivity;
import com.arohi.healthcare.activities.EmailVerificationActivity;
import com.arohi.healthcare.activities.LoginActivity;
import com.arohi.healthcare.activities.OnboardingActivity;
import com.arohi.healthcare.activities.RolePortalActivity;
import com.arohi.healthcare.activities.RoleSelectionActivity;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.utils.SessionManager;

/**
 * Central navigation coordinator for Phase 1 Authentication & Identity flows.
 * Ensures session persistence and prevents unverified professionals from bypassing verification checks.
 */
public final class AuthNavigationRouter {

    private AuthNavigationRouter() {
        // Prevent instantiation
    }

    /**
     * Routes the user from SplashActivity based on onboarding state and active session persistence.
     */
    public static void routeFromSplash(Activity activity, SessionManager sessionManager) {
        if (!sessionManager.isOnboardingCompleted()) {
            Intent intent = new Intent(activity, OnboardingActivity.class);
            activity.startActivity(intent);
            activity.finish();
            return;
        }

        UserAccount activeAccount = sessionManager.getCurrentSessionAccount();
        if (activeAccount != null) {
            routeAuthenticatedUser(activity, activeAccount);
        } else {
            Intent intent = new Intent(activity, RoleSelectionActivity.class);
            activity.startActivity(intent);
            activity.finish();
        }
    }

    /**
     * Routes an authenticated user to EmailVerificationActivity if their email is not yet verified,
     * or to BasicProfileSetupActivity (which serves as the Phase 1 Profile & Role Verification Hub).
     */
    public static void routeAuthenticatedUser(Activity activity, UserAccount account) {
        if (account == null) {
            Intent intent = new Intent(activity, LoginActivity.class);
            activity.startActivity(intent);
            activity.finish();
            return;
        }

        Intent intent;
        if (!account.isEmailVerified()) {
            intent = new Intent(activity, EmailVerificationActivity.class);
        } else {
            intent = new Intent(activity, RolePortalActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
