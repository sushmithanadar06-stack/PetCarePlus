package com.example.petcareplus.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * ImageView with pinch-to-zoom, double-tap zoom and panning.
 * Works inside a ViewPager2: while zoomed in it keeps the touch, and when you drag past the
 * image edge (or aren't zoomed) it lets the pager swipe to the next photo.
 */
public class ZoomImageView extends AppCompatImageView {

    private static final float MAX_ZOOM = 5f;       // relative to "fit to screen"
    private static final float DOUBLE_TAP_ZOOM = 2.5f;

    private final Matrix matrix = new Matrix();
    private final float[] v = new float[9];
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    private float baseScale = 1f;
    private float lastX, lastY;

    public ZoomImageView(Context context) {
        this(context, null);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);

        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector d) {
                        float current = currentScale();
                        float target = current * d.getScaleFactor();
                        target = Math.max(baseScale, Math.min(baseScale * MAX_ZOOM, target));
                        float factor = target / current;
                        matrix.postScale(factor, factor, d.getFocusX(), d.getFocusY());
                        applyMatrix();
                        return true;
                    }
                });

        gestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        float current = currentScale();
                        float target = current > baseScale * 1.05f
                                ? baseScale
                                : baseScale * DOUBLE_TAP_ZOOM;
                        float factor = target / current;
                        matrix.postScale(factor, factor, e.getX(), e.getY());
                        applyMatrix();
                        return true;
                    }
                });
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        if (getWidth() > 0) fitToView();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fitToView();
    }

    private void fitToView() {
        Drawable d = getDrawable();
        if (d == null || getWidth() == 0 || getHeight() == 0) return;

        float dw = d.getIntrinsicWidth();
        float dh = d.getIntrinsicHeight();
        if (dw <= 0 || dh <= 0) return;

        baseScale = Math.min(getWidth() / dw, getHeight() / dh);
        matrix.reset();
        matrix.postScale(baseScale, baseScale);
        matrix.postTranslate((getWidth() - dw * baseScale) / 2f,
                (getHeight() - dh * baseScale) / 2f);
        setImageMatrix(matrix);
    }

    private float currentScale() {
        matrix.getValues(v);
        return v[Matrix.MSCALE_X];
    }

    private boolean isZoomed() {
        return currentScale() > baseScale * 1.02f;
    }

    private void applyMatrix() {
        fixTranslation();
        setImageMatrix(matrix);
    }

    /** Keeps the image inside the view (or centred when smaller than the view). */
    private void fixTranslation() {
        Drawable d = getDrawable();
        if (d == null) return;

        matrix.getValues(v);
        float scale = v[Matrix.MSCALE_X];
        float w = d.getIntrinsicWidth() * scale;
        float h = d.getIntrinsicHeight() * scale;
        float tx = v[Matrix.MTRANS_X];
        float ty = v[Matrix.MTRANS_Y];

        float dx = 0f, dy = 0f;

        if (w <= getWidth()) {
            dx = (getWidth() - w) / 2f - tx;
        } else if (tx > 0) {
            dx = -tx;
        } else if (tx + w < getWidth()) {
            dx = getWidth() - (tx + w);
        }

        if (h <= getHeight()) {
            dy = (getHeight() - h) / 2f - ty;
        } else if (ty > 0) {
            dy = -ty;
        } else if (ty + h < getHeight()) {
            dy = getHeight() - (ty + h);
        }

        matrix.postTranslate(dx, dy);
    }

    /** True if the image can still be dragged sideways in the direction of dx. */
    private boolean canPanHorizontally(float dx) {
        Drawable d = getDrawable();
        if (d == null) return false;

        matrix.getValues(v);
        float w = d.getIntrinsicWidth() * v[Matrix.MSCALE_X];
        float tx = v[Matrix.MTRANS_X];

        if (dx > 0) return tx < -0.5f;                      // can still move right
        if (dx < 0) return tx + w > getWidth() + 0.5f;      // can still move left
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (getDrawable() == null) return super.onTouchEvent(ev);

        scaleDetector.onTouchEvent(ev);
        gestureDetector.onTouchEvent(ev);

        boolean zoomed = isZoomed();

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = ev.getX();
                lastY = ev.getY();
                requestParentIntercept(zoomed);
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                requestParentIntercept(true);
                break;

            case MotionEvent.ACTION_MOVE:
                if (ev.getPointerCount() == 1 && !scaleDetector.isInProgress() && zoomed) {
                    float dx = ev.getX() - lastX;
                    float dy = ev.getY() - lastY;
                    matrix.postTranslate(dx, dy);
                    applyMatrix();
                    // at the image edge -> let the pager swipe to the next photo
                    requestParentIntercept(canPanHorizontally(dx));
                } else if (ev.getPointerCount() > 1) {
                    requestParentIntercept(true);
                }
                lastX = ev.getX();
                lastY = ev.getY();
                break;

            case MotionEvent.ACTION_POINTER_UP:
                int remaining = ev.getActionIndex() == 0 ? 1 : 0;
                if (remaining < ev.getPointerCount()) {
                    lastX = ev.getX(remaining);
                    lastY = ev.getY(remaining);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                requestParentIntercept(false);
                break;

            default:
                break;
        }
        return true;
    }

    /** disallow = true means "keep the touch, don't let the parent (pager) take it". */
    private void requestParentIntercept(boolean disallow) {
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(disallow);
    }
}