package com.example.petcareplus.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.petcareplus.R;
import com.example.petcareplus.adapters.OnboardingAdapter;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPagerOnboarding;
    private LinearLayout dotsContainer;
    private Button btnOnboardingNext;
    private TextView txtOnboardingSkip;

    private int totalPages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPagerOnboarding = findViewById(R.id.viewPagerOnboarding);
        dotsContainer = findViewById(R.id.dotsContainer);
        btnOnboardingNext = findViewById(R.id.btnOnboardingNext);
        txtOnboardingSkip = findViewById(R.id.txtOnboardingSkip);

        OnboardingAdapter adapter = new OnboardingAdapter();
        viewPagerOnboarding.setAdapter(adapter);
        totalPages = adapter.getItemCount();

        setupDots();
        updateDots(0);

        viewPagerOnboarding.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateDots(position);

                boolean isLastPage = position == totalPages - 1;
                btnOnboardingNext.setText(isLastPage ? "Get Started" : "Continue →");
            }
        });

        btnOnboardingNext.setOnClickListener(v -> {

            int current = viewPagerOnboarding.getCurrentItem();

            if (current < totalPages - 1) {
                viewPagerOnboarding.setCurrentItem(current + 1);
            } else {
                finishOnboarding();
            }
        });

        txtOnboardingSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void setupDots() {

        dotsContainer.removeAllViews();

        for (int i = 0; i < totalPages; i++) {

            TextView dot = new TextView(this);
            dot.setText("●");
            dot.setTextSize(10);
            dot.setTextColor(0xFFDEE5DD);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(6, 0, 6, 0);
            dot.setLayoutParams(params);

            dotsContainer.addView(dot);
        }
    }

    private void updateDots(int activeIndex) {

        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            TextView dot = (TextView) dotsContainer.getChildAt(i);
            dot.setTextColor(i == activeIndex ? 0xFF2F6B52 : 0xFFDEE5DD);
        }
    }

    private void finishOnboarding() {

        SharedPreferences prefs = getSharedPreferences("petcare", MODE_PRIVATE);
        prefs.edit().putBoolean("onboarding_seen", true).apply();

        startActivity(new Intent(OnboardingActivity.this, LoginActivity.class));
        finish();
    }
}