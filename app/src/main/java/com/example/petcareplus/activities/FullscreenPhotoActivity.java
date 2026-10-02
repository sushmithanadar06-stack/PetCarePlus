package com.example.petcareplus.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.utils.ImageUtils;
import com.example.petcareplus.views.ZoomableImageView;

public class FullscreenPhotoActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTO_BASE64 = "photoBase64";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_photo);

        ZoomableImageView imgFullscreenPhoto = findViewById(R.id.imgFullscreenPhoto);
        TextView btnCloseFullscreen = findViewById(R.id.btnCloseFullscreen);

        String base64 = getIntent().getStringExtra(EXTRA_PHOTO_BASE64);

        if (base64 != null && ImageUtils.isBase64Image(base64)) {
            Bitmap bitmap = ImageUtils.base64ToBitmap(base64);
            if (bitmap != null) {
                imgFullscreenPhoto.setImageBitmap(bitmap);
            }
        }

        btnCloseFullscreen.setOnClickListener(v -> finish());
    }
}