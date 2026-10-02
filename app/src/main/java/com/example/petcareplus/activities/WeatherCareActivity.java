package com.example.petcareplus.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.WeatherInfo;
import com.example.petcareplus.utils.WeatherCareTips;
import com.example.petcareplus.utils.WeatherFetcher;

import java.util.List;

public class WeatherCareActivity extends AppCompatActivity {

    private TextView btnWeatherBack, txtWeatherTemp, txtWeatherCondition;
    private LinearLayout tipsContainer;

    private String petType = "Dog";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_care);

        btnWeatherBack = findViewById(R.id.btnWeatherBack);
        txtWeatherTemp = findViewById(R.id.txtWeatherTemp);
        txtWeatherCondition = findViewById(R.id.txtWeatherCondition);
        tipsContainer = findViewById(R.id.tipsContainer);

        if (getIntent().hasExtra("petType")) {
            petType = getIntent().getStringExtra("petType");
        }

        btnWeatherBack.setOnClickListener(v -> finish());

        loadWeather();
    }

    private void loadWeather() {

        WeatherFetcher.fetchMumbaiWeather(new WeatherFetcher.WeatherCallback() {

            @Override
            public void onSuccess(WeatherInfo weather) {

                txtWeatherTemp.setText(Math.round(weather.temperature) + "°C");
                txtWeatherCondition.setText(weather.condition + " · Mumbai");

                renderTips(weather);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(WeatherCareActivity.this,
                        "Failed to load weather: " + errorMessage,
                        Toast.LENGTH_LONG).show();
                txtWeatherCondition.setText("Unable to load · Mumbai");
            }
        });
    }

    private void renderTips(WeatherInfo weather) {

        tipsContainer.removeAllViews();

        List<WeatherCareTips.Tip> tips = WeatherCareTips.getTips(weather, petType);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (WeatherCareTips.Tip tip : tips) {

            View tipView = inflater.inflate(R.layout.item_weather_tip, tipsContainer, false);

            TextView txtIcon = tipView.findViewById(R.id.txtTipIcon);
            TextView txtTitle = tipView.findViewById(R.id.txtTipTitle);
            TextView txtDesc = tipView.findViewById(R.id.txtTipDescription);

            txtIcon.setText(tip.icon);
            txtTitle.setText(tip.title);
            txtDesc.setText(tip.description);

            tipsContainer.addView(tipView);
        }
    }
}