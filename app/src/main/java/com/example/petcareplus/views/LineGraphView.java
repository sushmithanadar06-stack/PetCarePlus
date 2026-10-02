package com.example.petcareplus.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class LineGraphView extends View {

    // The values actually drawn, after trimming leading no-data days and
    // carrying the last known score forward through gap days (like a
    // stock chart holds flat on non-trading days instead of dropping to 0).
    private final List<Integer> plotValues = new ArrayList<>();
    private final List<String> plotLabels = new ArrayList<>();
    private boolean hasAnyData = false;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint yLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint xLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyStatePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int LINE_COLOR = Color.parseColor("#1E5C45");
    private static final int GRID_COLOR = Color.parseColor("#E5E9E3");
    private static final int LABEL_COLOR = Color.parseColor("#9C93AD");

    public LineGraphView(Context context) { super(context); init(); }
    public LineGraphView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        linePaint.setColor(LINE_COLOR);
        linePaint.setStrokeWidth(6f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);

        dotPaint.setColor(LINE_COLOR);
        dotPaint.setStyle(Paint.Style.FILL);

        dotRingPaint.setColor(Color.WHITE);
        dotRingPaint.setStyle(Paint.Style.FILL);

        gridPaint.setColor(GRID_COLOR);
        gridPaint.setStrokeWidth(2f);
        gridPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{8f, 8f}, 0));

        yLabelPaint.setColor(LABEL_COLOR);
        yLabelPaint.setTextSize(22f);

        xLabelPaint.setColor(LABEL_COLOR);
        xLabelPaint.setTextSize(20f);
        xLabelPaint.setTextAlign(Paint.Align.CENTER);

        emptyStatePaint.setColor(LABEL_COLOR);
        emptyStatePaint.setTextSize(26f);
        emptyStatePaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * @param scores one entry per day, oldest to newest. Use null for days
     *               with no logged data at all — this view will trim any
     *               leading run of nulls and carry the last known score
     *               forward through any nulls that follow real data.
     * @param labels short display label per day (e.g. "Aug 12"), same
     *               length and order as scores.
     */
    public void setData(List<Integer> scores, List<String> labels) {

        plotValues.clear();
        plotLabels.clear();

        Integer lastKnown = null;
        boolean started = false;

        int n = Math.min(scores.size(), labels.size());

        for (int i = 0; i < n; i++) {
            Integer value = scores.get(i);
            if (value != null) {
                lastKnown = value;
                started = true;
            }
            if (started) {
                plotValues.add(lastKnown);
                plotLabels.add(labels.get(i));
            }
        }

        hasAnyData = started;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int leftPad = 56;
        int rightPad = 20;
        int topPad = 24;
        int bottomPad = 44;

        int[] gridValues = {0, 50, 100};
        for (int value : gridValues) {
            float y = topPad + (height - topPad - bottomPad) * (1 - value / 100f);
            canvas.drawLine(leftPad, y, width - rightPad, y, gridPaint);
            canvas.drawText(String.valueOf(value), 4, y + 8, yLabelPaint);
        }

        if (!hasAnyData || plotValues.isEmpty()) {
            canvas.drawText("No data logged yet", width / 2f, height / 2f, emptyStatePaint);
            return;
        }

        int n = plotValues.size();

        if (n == 1) {
            float x = (width - rightPad + leftPad) / 2f;
            float y = topPad + (height - topPad - bottomPad) * (1 - clamp(plotValues.get(0)) / 100f);
            canvas.drawCircle(x, y, 10f, dotRingPaint);
            canvas.drawCircle(x, y, 7f, dotPaint);
            canvas.drawText(plotLabels.get(0), x, height - 12, xLabelPaint);
            return;
        }

        float[] xs = new float[n];
        float[] ys = new float[n];
        float stepX = (width - leftPad - rightPad) / (float) (n - 1);

        for (int i = 0; i < n; i++) {
            xs[i] = leftPad + i * stepX;
            float normalized = clamp(plotValues.get(i)) / 100f;
            ys[i] = topPad + (height - topPad - bottomPad) * (1 - normalized);
        }

        // Smooth curve through the points
        Path linePath = new Path();
        linePath.moveTo(xs[0], ys[0]);
        for (int i = 0; i < n - 1; i++) {
            float midX = (xs[i] + xs[i + 1]) / 2f;
            float midY = (ys[i] + ys[i + 1]) / 2f;
            linePath.quadTo(xs[i], ys[i], midX, midY);
        }
        linePath.lineTo(xs[n - 1], ys[n - 1]);

        // Gradient fill under the curve — stock-chart look
        Path fillPath = new Path(linePath);
        fillPath.lineTo(xs[n - 1], height - bottomPad);
        fillPath.lineTo(xs[0], height - bottomPad);
        fillPath.close();

        fillPaint.setShader(new LinearGradient(
                0, topPad, 0, height - bottomPad,
                Color.parseColor("#401E5C45"),
                Color.parseColor("#001E5C45"),
                Shader.TileMode.CLAMP
        ));
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);

        // Highlight latest point
        canvas.drawCircle(xs[n - 1], ys[n - 1], 10f, dotRingPaint);
        canvas.drawCircle(xs[n - 1], ys[n - 1], 7f, dotPaint);

        // X-axis day labels — show at most ~5 to avoid crowding, always
        // including the first and last day
        int maxLabels = 5;
        int labelStep = Math.max(1, (int) Math.ceil(n / (double) maxLabels));

        for (int i = 0; i < n; i++) {
            if (i == 0 || i == n - 1 || i % labelStep == 0) {
                canvas.drawText(plotLabels.get(i), xs[i], height - 10, xLabelPaint);
            }
        }
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}