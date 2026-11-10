package com.arohi.healthcare.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.OnboardingAdapter;
import com.arohi.healthcare.databinding.ActivityOnboardingBinding;
import com.arohi.healthcare.models.OnboardingItem;
import com.arohi.healthcare.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;

/**
 * 2. Onboarding Screens
 * Introduces the AROHI connected healthcare ecosystem with a 3-step ViewPager2 experience.
 */
public class OnboardingActivity extends BaseActivity {

    private ActivityOnboardingBinding binding;
    private SessionManager sessionManager;
    private List<OnboardingItem> pages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        setupOnboardingPages();
        setupListeners();
    }

    private void setupOnboardingPages() {
        pages = new ArrayList<>();
        pages.add(new OnboardingItem(
                R.drawable.ic_arohi_logo,
                getString(R.string.onboarding_badge_1),
                getString(R.string.onboarding_title_1),
                getString(R.string.onboarding_desc_1)
        ));
        pages.add(new OnboardingItem(
                R.drawable.ic_role_doctor,
                getString(R.string.onboarding_badge_2),
                getString(R.string.onboarding_title_2),
                getString(R.string.onboarding_desc_2)
        ));
        pages.add(new OnboardingItem(
                R.drawable.ic_verified_badge,
                getString(R.string.onboarding_badge_3),
                getString(R.string.onboarding_title_3),
                getString(R.string.onboarding_desc_3)
        ));

        OnboardingAdapter adapter = new OnboardingAdapter(pages);
        binding.viewPagerOnboarding.setAdapter(adapter);

        binding.viewPagerOnboarding.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicatorDots(position);
                if (position == pages.size() - 1) {
                    binding.btnNextOrStart.setText(R.string.action_get_started);
                } else {
                    binding.btnNextOrStart.setText(R.string.action_next);
                }
            }
        });
    }

    private void updateIndicatorDots(int activePosition) {
        binding.dot1.setBackgroundResource(activePosition == 0 ? R.drawable.dot_indicator_active : R.drawable.dot_indicator_inactive);
        binding.dot2.setBackgroundResource(activePosition == 1 ? R.drawable.dot_indicator_active : R.drawable.dot_indicator_inactive);
        binding.dot3.setBackgroundResource(activePosition == 2 ? R.drawable.dot_indicator_active : R.drawable.dot_indicator_inactive);
    }

    private void setupListeners() {
        binding.btnSkip.setOnClickListener(v -> completeOnboardingAndProceed());

        binding.btnNextOrStart.setOnClickListener(v -> {
            int current = binding.viewPagerOnboarding.getCurrentItem();
            if (current < pages.size() - 1) {
                binding.viewPagerOnboarding.setCurrentItem(current + 1, true);
            } else {
                completeOnboardingAndProceed();
            }
        });
    }

    private void completeOnboardingAndProceed() {
        sessionManager.setOnboardingCompleted(true);
        Intent intent = new Intent(this, HospitalDiscoveryActivity.class);
        startActivity(intent);
        finish();
    }
}
