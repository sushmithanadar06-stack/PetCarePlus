package com.example.petcareplus.views;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

public class ZoomableImageView extends androidx.appcompat.widget.AppCompatImageView {

    private final Matrix matrix = new Matrix();
    private float scale = 1f;
    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 5f;

    private float lastTouchX, lastTouchY;
    private float posX = 0f, posY = 0f;
    private boolean isDragging = false;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    public ZoomableImageView(Context context) { super(context); init(context); }
    public ZoomableImageView(Context context, AttributeSet attrs) { super(context, attrs); init(context); }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float newScale = scale * detector.getScaleFactor();
                scale = Math.max(MIN_SCALE, Math.min(newScale, MAX_SCALE));
                applyMatrix();
                return true;
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                scale = (scale > MIN_SCALE) ? MIN_SCALE : 3f;
                if (scale == MIN_SCALE) { posX = 0; posY = 0; }
                applyMatrix();
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isDragging = true;
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging && scale > MIN_SCALE && event.getPointerCount() == 1) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    posX += dx;
                    posY += dy;
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    applyMatrix();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                isDragging = false;
                break;
        }

        return true;
    }

    private void applyMatrix() {
        matrix.reset();

        Drawable d = getDrawable();
        if (d == null) return;

        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float drawableWidth = d.getIntrinsicWidth();
        float drawableHeight = d.getIntrinsicHeight();

        if (drawableWidth == 0 || drawableHeight == 0) return;

        float baseScale = Math.min(viewWidth / drawableWidth, viewHeight / drawableHeight);
        float dx = (viewWidth - drawableWidth * baseScale) / 2f;
        float dy = (viewHeight - drawableHeight * baseScale) / 2f;

        matrix.postScale(baseScale * scale, baseScale * scale);
        matrix.postTranslate(dx + posX, dy + posY);

        setImageMatrix(matrix);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        applyMatrix();
    }

    @Override
    public void setImageBitmap(android.graphics.Bitmap bm) {
        super.setImageBitmap(bm);
        scale = 1f;
        posX = 0f;
        posY = 0f;
        applyMatrix();
    }
}