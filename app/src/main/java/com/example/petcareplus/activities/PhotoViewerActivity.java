package com.example.petcareplus.activities;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.utils.ImageUtils;

public class PhotoViewerActivity extends AppCompatActivity {

    private ImageView imgFullPhoto;
    private TextView btnClosePhotoViewer;

    private final Matrix matrix = new Matrix();
    private float scaleFactor = 1f;

    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 5f;

    private ScaleGestureDetector scaleGestureDetector;
    private float lastTouchX, lastTouchY;
    private boolean isDragging = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_viewer);

        imgFullPhoto = findViewById(R.id.imgFullPhoto);
        btnClosePhotoViewer = findViewById(R.id.btnClosePhotoViewer);

        String base64 = getIntent().getStringExtra("photoBase64");

        if (base64 == null || base64.isEmpty()) {
            Toast.makeText(this, "Unable to load photo", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Bitmap bitmap = ImageUtils.base64ToBitmap(base64);

        if (bitmap == null) {
            Toast.makeText(this, "Unable to load photo", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        imgFullPhoto.setImageBitmap(bitmap);
        imgFullPhoto.setScaleType(ImageView.ScaleType.MATRIX);

        // Wait for the view to be laid out so we know its real size,
        // then fit the image centered within it — like a proper photo
        // viewer's starting state, not a raw 1:1 pixel dump in the corner.
        imgFullPhoto.post(() -> fitImageToScreen(bitmap));

        btnClosePhotoViewer.setOnClickListener(v -> finish());

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        imgFullPhoto.setOnTouchListener((v, event) -> {

            scaleGestureDetector.onTouchEvent(event);

            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    isDragging = true;
                    break;

                case MotionEvent.ACTION_MOVE:
                    // Only pan with a single finger, and only once zoomed in —
                    // otherwise a one-finger drag on a fitted image feels broken.
                    if (isDragging && event.getPointerCount() == 1 && scaleFactor > MIN_SCALE) {
                        float dx = event.getX() - lastTouchX;
                        float dy = event.getY() - lastTouchY;
                        matrix.postTranslate(dx, dy);
                        imgFullPhoto.setImageMatrix(matrix);
                        lastTouchX = event.getX();
                        lastTouchY = event.getY();
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    isDragging = false;
                    break;
            }

            return true;
        });
    }

    private void fitImageToScreen(Bitmap bitmap) {

        int viewWidth = imgFullPhoto.getWidth();
        int viewHeight = imgFullPhoto.getHeight();

        if (viewWidth == 0 || viewHeight == 0) return;

        float scale = Math.min(
                (float) viewWidth / bitmap.getWidth(),
                (float) viewHeight / bitmap.getHeight()
        );

        float dx = (viewWidth - bitmap.getWidth() * scale) / 2f;
        float dy = (viewHeight - bitmap.getHeight() * scale) / 2f;

        matrix.reset();
        matrix.postScale(scale, scale);
        matrix.postTranslate(dx, dy);

        imgFullPhoto.setImageMatrix(matrix);
        scaleFactor = 1f;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            float newScaleFactor = scaleFactor * detector.getScaleFactor();
            newScaleFactor = Math.max(MIN_SCALE, Math.min(newScaleFactor, MAX_SCALE));

            float appliedScale = newScaleFactor / scaleFactor;
            scaleFactor = newScaleFactor;

            matrix.postScale(
                    appliedScale, appliedScale,
                    detector.getFocusX(), detector.getFocusY()
            );

            imgFullPhoto.setImageMatrix(matrix);
            return true;
        }
    }
}