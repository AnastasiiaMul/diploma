package com.example.diplomaappmodeltflite;

public class DetectionResult {
    public int objectId; // Unique ID for tracking
    public final int detectedClass;
    public final float confidence;
    public final float left, top, right, bottom;

    public DetectionResult(int objectId, int detectedClass, float confidence,
                           float left, float top, float right, float bottom) {
        this.objectId = objectId;
        this.detectedClass = detectedClass;
        this.confidence = confidence;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    //intersection over union (overlap between 2 boxes)
    public float iou(DetectionResult other) {
        float intersectLeft = Math.max(this.left, other.left);
        float intersectTop = Math.max(this.top, other.top);
        float intersectRight = Math.min(this.right, other.right);
        float intersectBottom = Math.min(this.bottom, other.bottom);

        float intersectArea = Math.max(0, intersectRight - intersectLeft) *
                Math.max(0, intersectBottom - intersectTop);

        float area1 = (this.right - this.left) * (this.bottom - this.top);
        float area2 = (other.right - other.left) * (other.bottom - other.top);

        return intersectArea / (area1 + area2 - intersectArea);
    }

}
