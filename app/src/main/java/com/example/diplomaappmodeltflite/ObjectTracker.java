package com.example.diplomaappmodeltflite;

import java.util.HashMap;
import java.util.Map;

public class ObjectTracker {
    private final Map<Integer, float[]> trackedObjects = new HashMap<>();
    private int nextObjectId = 1;

    public int getTrackedObjectId(float centerX, float centerY) {
        int assignedId = -1;
        double minDistance = Double.MAX_VALUE;

        // Find the closest existing object
        for (Map.Entry<Integer, float[]> entry : trackedObjects.entrySet()) {
            float[] previousCenter = entry.getValue();
            double distance = Math.sqrt(Math.pow(centerX - previousCenter[0], 2) + Math.pow(centerY - previousCenter[1], 2));

            if (distance < minDistance && distance < 50) { // Threshold for object re-association
                assignedId = entry.getKey();
                minDistance = distance;
            }
        }

        if (assignedId == -1) {
            assignedId = nextObjectId++;
        }

        // Update object position
        trackedObjects.put(assignedId, new float[]{centerX, centerY});
        return assignedId;
    }
}
