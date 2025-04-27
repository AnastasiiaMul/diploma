package com.example.diplomaappmodeltflite;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;
public class OverlayView extends View {

    private List<DetectionResult> results;
    private Paint boxPaint;
    private Paint textPaint;
    private int modelInputWidth = 640;
    private int modelInputHeight = 640;
    private int previewWidth = 1080;
    private int previewHeight = 2400;

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
        if (results != null) {
            float scaleX = (float) getWidth() / modelInputWidth;
            float scaleY = (float) getHeight() / modelInputHeight;


            for (DetectionResult result : results) {
                // Draw bounding box
                float left = result.left * scaleX;
                float top = result.top * scaleY;
                float right = result.right * scaleX;
                float bottom = result.bottom * scaleY;

                canvas.drawRect(left, top, right, bottom, boxPaint);

                // Display label: "#ID Class (Confidence)"
                String className = CocoLabels.LABELS[result.detectedClass]; //diplays cocc
                //String className = CocoLabels.LABELS_MODEL1[result.detectedClass]; //diplays trained model
                String label = "#" + result.objectId + " " + className +
                        " (" + String.format("%.1f", result.confidence * 100) + "%)";

                // Draw background for label
                float textWidth = textPaint.measureText(label);
                float textSize = textPaint.getTextSize();
                canvas.drawRect(left, top - textSize - 10, left + textWidth + 10, top, boxPaint);

                // Draw text label
                canvas.drawText(label, left + 5, top - 10, textPaint);            }
        }
    }
}
