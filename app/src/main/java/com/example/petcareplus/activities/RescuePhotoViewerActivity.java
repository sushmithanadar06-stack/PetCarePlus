package com.example.petcareplus.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.petcareplus.R;
import com.example.petcareplus.models.RescueReport;
import com.example.petcareplus.utils.RescueImageUtils;
import com.example.petcareplus.utils.ZoomImageView;

import java.util.List;

/**
 * Full-screen viewer: swipe between the report's photos, pinch or double-tap to zoom.
 * Extras: "reportId" (String), "index" (int, first photo to show).
 */
public class RescuePhotoViewerActivity extends AppCompatActivity {

    /** Set by the caller right before starting this screen (avoids re-downloading the photos). */
    public static List<String> pending;

    private ViewPager2 pager;
    private TextView txtCounter;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescue_photo_viewer);

        pager = findViewById(R.id.pagerPhotos);
        txtCounter = findViewById(R.id.txtCounter);
        progress = findViewById(R.id.progress);
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        final int startIndex = getIntent().getIntExtra("index", 0);
        String reportId = getIntent().getStringExtra("reportId");

        if (pending != null && !pending.isEmpty()) {
            show(pending, startIndex);
        } else if (reportId != null) {
            RescueReport.loadPhotos(reportId, new RescueReport.PhotosCallback() {
                @Override
                public void onLoaded(List<String> photos) {
                    if (isFinishing()) return;
                    if (photos.isEmpty()) {
                        Toast.makeText(RescuePhotoViewerActivity.this, "No photos found",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        show(photos, startIndex);
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (isFinishing()) return;
                    Toast.makeText(RescuePhotoViewerActivity.this,
                            "Couldn't load photos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        } else {
            finish();
        }
    }

    private void show(List<String> photos, int startIndex) {
        progress.setVisibility(View.GONE);

        final int total = photos.size();
        pager.setAdapter(new PhotoAdapter(photos));
        pager.setOffscreenPageLimit(1);

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                txtCounter.setText((position + 1) + " / " + total);
            }
        });

        int index = Math.max(0, Math.min(startIndex, total - 1));
        pager.setCurrentItem(index, false);
        txtCounter.setText((index + 1) + " / " + total);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) pending = null;
    }

    // ------------------------------------------------------------------ adapter

    private static class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.Holder> {

        private final List<String> data;

        PhotoAdapter(List<String> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ZoomImageView view = new ZoomImageView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Bitmap bmp = RescueImageUtils.base64ToBitmap(data.get(position));
            holder.view.setImageBitmap(bmp);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final ZoomImageView view;

            Holder(@NonNull ZoomImageView itemView) {
                super(itemView);
                this.view = itemView;
            }
        }
    }
}