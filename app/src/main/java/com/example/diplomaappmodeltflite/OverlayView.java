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
        invalidate(); // refresh the view clearly
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (results != null) {
            for (DetectionResult result : results) {
                // Draw bounding box
                canvas.drawRect(result.left, result.top, result.right, result.bottom, boxPaint);

                // Display label: "#ID Class (Confidence)"
                String className = CocoLabels.LABELS[result.detectedClass]; //diplays cocc
                //String className = CocoLabels.LABELS_MODEL1[result.detectedClass]; //diplays trained model
                String label = "#" + result.objectId + " " + className +
                        " (" + String.format("%.1f", result.confidence * 100) + "%)";

                // Draw background for label
                float textWidth = textPaint.measureText(label);
                float textSize = textPaint.getTextSize();
                canvas.drawRect(result.left, result.top - textSize - 10, result.left + textWidth + 10, result.top, boxPaint);

                // Draw text label
                canvas.drawText(label, result.left + 5, result.top - 10, textPaint);
            }
        }
    }
}
