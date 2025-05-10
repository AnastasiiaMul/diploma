package com.example.diplomaappmodeltflite;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.List;
public class OverlayView extends View {

    private List<DetectionResult> results;
    private Paint boxPaint;
    private Paint textPaint;
    private int modelInputWidth = 640;
    private int modelInputHeight = 640;
    private int previewWidth = 1280;
    private int previewHeight = 720;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStrokeWidth(6f);
        boxPaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint();
        textPaint.setColor(Color.GREEN);
        textPaint.setTextSize(36f);
    }

    public void setResults(List<DetectionResult> results) {
        this.results = results;
        invalidate(); // refresh the view
    }

    public void setPreviewSize(int previewWidth, int previewHeight) {
        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;
    }

    public void setModelInputSize(int modelInputWidth, int modelInputHeight) {
        this.modelInputWidth = modelInputWidth;
        this.modelInputHeight = modelInputHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        Log.d("OverlayView", "Drawing on canvas: view=" + viewWidth + "x" + viewHeight +
                ", preview=" + previewWidth + "x" + previewHeight);

        if (results != null) {
            float viewRatio = viewWidth / viewHeight;
            float modelRatio = (float) modelInputWidth / modelInputHeight;

            float scale;
            float dx = 0, dy = 0;

            if (viewRatio > modelRatio) {
                scale = viewHeight / modelInputHeight;
                dx = (viewWidth - modelInputWidth * scale) / 2;
            } else {
                scale = viewWidth / modelInputWidth;
                dy = (viewHeight - modelInputHeight * scale) / 2;
            }

            // Draw debug frame
            Paint debugPaint = new Paint();
            debugPaint.setColor(Color.RED);
            debugPaint.setStrokeWidth(4f);
            debugPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(dx, dy, dx + modelInputWidth * scale, dy + modelInputHeight * scale, debugPaint);

            for (DetectionResult result : results) {
                float left = result.left * scale + dx;
                float top = result.top * scale + dy;
                float right = result.right * scale + dx;
                float bottom = result.bottom * scale + dy;

                canvas.drawRect(left, top, right, bottom, boxPaint);

                String className = CocoLabels.LABELS[result.detectedClass];
                String label = "#" + result.objectId + " " + className +
                        " (" + String.format("%.1f", result.confidence * 100) + "%)";

                float textWidth = textPaint.measureText(label);
                float textSize = textPaint.getTextSize();
                canvas.drawRect(left, top - textSize - 10, left + textWidth + 10, top, boxPaint);
                canvas.drawText(label, left + 5, top - 10, textPaint);
            }
        }
    }
}
