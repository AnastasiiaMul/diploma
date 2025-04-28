package com.example.diplomaappmodeltflite;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.SoundPool;
import android.util.Log;
import android.widget.TextView;

import androidx.camera.view.PreviewView;

import org.tensorflow.lite.Interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DetectionProcessor {

    private final Context context;
    private final Interpreter interpreter;
    private final OverlayView overlayView;
    private final TextView detectionResultsTextView;
    private final SoundPool soundPool;
    private final Map<Integer, Integer> sectorSoundMap;
    private final SessionLogger sessionLogger;
    private final ObjectTracker objectTracker = new ObjectTracker();

    private static final int NUM_CLASSES = 80;
    private static final float CONFIDENCE_THRESHOLD = 0.8f;
    private static final float NMS_THRESHOLD = 0.5f;
    private static final float FOCAL_LENGTH = 500f;
    private boolean isClosed = false;

    // New for frequency management
    private final Map<Integer, Long> lastPlayedTimePerObject = new HashMap<>();
    private long lastGlobalSoundTime = 0L;
    private int objectSoundCooldownMs = 2000;  // Default 2 seconds
    private int globalSoundPauseMs = 500;      // Default 0.5 seconds
    private String detectionIntervalMode = "Seconds"; // Default detection mode
    private int detectionIntervalValue = 10;   // Default 10 seconds or frames
    private long lastDetectionTimestamp = 0;
    private int frameCounter = 0;


    private static final Map<Integer, Float> OBJECT_HEIGHTS = new HashMap<Integer, Float>() {{
        put(0, 1.7f); // person
        put(1, 1.5f); // bicycle
        put(2, 1.5f); // car
    }};

    public DetectionProcessor(
            Context context,
            ObjectDetectorHelper objectDetectorHelper,
            OverlayView overlayView,
            TextView detectionResultsTextView,
            SoundPool soundPool,
            Map<Integer, Integer> sectorSoundMap,
            SessionLogger sessionLogger
    ) {
        this.context = context;
        this.interpreter = objectDetectorHelper.getInterpreter();
        this.overlayView = overlayView;
        this.detectionResultsTextView = detectionResultsTextView;
        this.soundPool = soundPool;
        this.sectorSoundMap = sectorSoundMap;
        this.sessionLogger = sessionLogger;

        SharedPreferences prefs = context.getSharedPreferences("FrequencyPrefs", Context.MODE_PRIVATE);
        objectSoundCooldownMs = prefs.getInt("object_sound_cooldown_ms", 2000);
        globalSoundPauseMs = prefs.getInt("global_sound_pause_ms", 500);
        detectionIntervalValue = prefs.getInt("detection_interval_value", 10);
        detectionIntervalMode = prefs.getString("detection_interval_mode", "Seconds");

    }

    public void close() {
        isClosed = true;
        if (interpreter != null) {
            interpreter.close();
            Log.d("DetectionProcessor", "Interpreter closed.");
        }
    }

    public void process(Bitmap bitmap) {

        if (isClosed || interpreter == null) {
            Log.w("DetectionProcessor", "Attempted to process with a closed or null interpreter.");
            return;
        }
        // Detection interval logic
        frameCounter++;
        long currentTime = System.currentTimeMillis();

        if (detectionIntervalMode.equals("Frames")) {
            if (frameCounter % detectionIntervalValue != 0) {
                return;
            }
        } else { // Seconds mode
            if (currentTime - lastDetectionTimestamp < detectionIntervalValue * 1000L) {
                return;
            }
            lastDetectionTimestamp = currentTime;
        }

        if (interpreter == null) {
            Log.e("DetectionProcessor", "Interpreter is not initialized.");
            return;
        }

        float[][][][] inputBuffer = CameraUtils.bitmapToInputArray(bitmap);
        float[][][] outputBuffer = new float[1][84][8400];

        // added to fix bug Internal error: The Interpreter has already been closed.
        try {
            interpreter.run(inputBuffer, outputBuffer);
        }catch (IllegalStateException e) {
            Log.e("DetectionProcessor", "Interpreter already closed", e);
        }

        List<DetectionResult> detections = parseYoloOutput(outputBuffer, bitmap.getWidth(), bitmap.getHeight());
        List<DetectionResult> finalDetections = applyNMS(detections);

        overlayView.setResults(finalDetections);

        StringBuilder resultText = new StringBuilder();

        for (DetectionResult det : finalDetections) {
            float boxHeight = det.bottom - det.top;
            float distance = -1f;

            if (OBJECT_HEIGHTS.containsKey(det.detectedClass)) {
                float realHeight = OBJECT_HEIGHTS.get(det.detectedClass);
                distance = Math.max(0, (realHeight * FOCAL_LENGTH) / (boxHeight * 0.5f));
            }

            float objectCenterX = (det.left + det.right) / 2f;
            int sectorCount = SectorSoundManager.getNumberOfSectors(context);
            int sectorId = (int) ((objectCenterX / bitmap.getWidth()) * sectorCount) + 1;
            sectorId = Math.min(sectorId, sectorCount);

            long now = System.currentTimeMillis();
            boolean canPlayGlobal = now - lastGlobalSoundTime > globalSoundPauseMs;
            Long lastObjectSoundTime = lastPlayedTimePerObject.get(det.objectId);
            boolean canPlayObject = lastObjectSoundTime == null || (now - lastObjectSoundTime > objectSoundCooldownMs);


            if (canPlayGlobal && canPlayObject) {
                // Sector sound
                Integer sectorSoundId = sectorSoundMap.get(sectorId);
                if (sectorSoundId != null) {
                    float quietVolume = 0.05f;
                    soundPool.play(sectorSoundId, quietVolume, quietVolume, 1, 0, 1.0f);
                    Log.d("SOUND", "Played sector sound for sector " + sectorId);
                }

                // Object sound
                String objectName = CocoLabels.getLabel(det.detectedClass);
                String objectSound = ObjectSoundPreferences.getSoundForObject(context, objectName);

                if (objectSound != null && !objectSound.isEmpty()) {
                    int resId = context.getResources().getIdentifier(objectSound.toLowerCase(), "raw", context.getPackageName());
                    if (resId != 0) {
                        int objectSoundId = soundPool.load(context, resId, 1);
                        float volume = DistanceSettingsActivity.getVolumeForDistance(context, distance);
                        soundPool.play(objectSoundId, volume, volume, 1, 0, 1.0f);
                        Log.d("SOUND", String.format(Locale.US, "Played sound for %s | Distance %.2f m | Volume %.2f", objectName, distance, volume));
                    }
                }

                lastPlayedTimePerObject.put(det.objectId, now);
                lastGlobalSoundTime = now;
            }

            resultText.append(String.format(Locale.US,
                    "ID %d | Class %d | %.1f%% | Sector %d | Distance: %.2f m\n",
                    det.objectId, det.detectedClass, det.confidence * 100, sectorId, distance));

            /*sessionLogger.log("Detected object ID=" + det.objectId +
                    ", Class=" + det.detectedClass +
                    ", Confidence=" + det.confidence);*/
        }

        detectionResultsTextView.post(() -> detectionResultsTextView.setText(resultText.toString()));
    }

    private float estimateDistance(DetectionResult det) {
        float boxHeight = det.bottom - det.top;
        if (OBJECT_HEIGHTS.containsKey(det.detectedClass)) {
            float realHeight = OBJECT_HEIGHTS.get(det.detectedClass);
            return Math.max(0, (realHeight * FOCAL_LENGTH) / (boxHeight * 0.5f));
        }
        return -1f;
    }

    private int calculateSector(DetectionResult det, float imageWidth) {
        float centerX = (det.left + det.right) / 2f;
        int sectorCount = SectorSoundManager.getNumberOfSectors(context);
        int sectorId = (int) ((centerX / imageWidth) * sectorCount) + 1;
        return Math.min(sectorId, sectorCount);
    }

    private List<DetectionResult> parseYoloOutput(float[][][] output, float imageWidth, float imageHeight) {
        List<DetectionResult> results = new ArrayList<>();

        for (int i = 0; i < output[0][0].length; i++) {
            float x = output[0][0][i];
            float y = output[0][1][i];
            float w = output[0][2][i];
            float h = output[0][3][i];

            int bestClass = -1;
            float bestConfidence = -1f;
            for (int c = 0; c < NUM_CLASSES; c++) {
                float conf = output[0][4 + c][i];
                if (conf > bestConfidence) {
                    bestConfidence = conf;
                    bestClass = c;
                }
            }

            if (bestConfidence > CONFIDENCE_THRESHOLD) {
                float left = (x - w / 2f) * imageWidth;
                float top = (y - h / 2f) * imageHeight;
                float right = (x + w / 2f) * imageWidth;
                float bottom = (y + h / 2f) * imageHeight;

                float centerX = (left + right) / 2;
                float centerY = (top + bottom) / 2;
                int objectId = objectTracker.getTrackedObjectId(centerX, centerY);

                results.add(new DetectionResult(objectId, bestClass, bestConfidence, left, top, right, bottom));
            }
        }

        return results;
    }

    private List<DetectionResult> applyNMS(List<DetectionResult> detections) {
        List<DetectionResult> nmsResults = new ArrayList<>();

        detections.sort((d1, d2) -> Float.compare(d2.confidence, d1.confidence));
        while (!detections.isEmpty()) {
            DetectionResult best = detections.remove(0);
            nmsResults.add(best);
            detections.removeIf(det -> det.detectedClass == best.detectedClass && best.iou(det) > NMS_THRESHOLD);
        }

        return nmsResults;
    }
}

