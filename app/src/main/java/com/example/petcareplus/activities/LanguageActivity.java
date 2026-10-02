package com.example.petcareplus.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;

public class LanguageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        TextView btnLanguageBack = findViewById(R.id.btnLanguageBack);
        btnLanguageBack.setOnClickListener(v -> finish());
    }
}