package com.example.diplomaappmodeltflite;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.camera.view.PreviewView;

import org.tensorflow.lite.Interpreter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private MediaPlayer mediaPlayer;
    // New for frequency management
    private final Map<Integer, Long> lastPlayedTimePerObject = new HashMap<>();
    private long lastGlobalSoundTime = 0L;
    private int objectSoundCooldownMs = 2000;  // Default 2 seconds
    private int globalSoundPauseMs = 500;      // Default 0.5 seconds
    private String detectionIntervalMode = "Seconds"; // Default detection mode
    private int detectionIntervalValue = 10;   // Default 10 seconds or frames
    private long lastDetectionTimestamp = 0;
    private int frameCounter = 0;
    private final Set<Integer> loadedSoundIds = new HashSet<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isPlayingSoundPair = false;
    private final Deque<SoundTask> pendingSounds = new ArrayDeque<>();




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

        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            if (status == 0) loadedSoundIds.add(sampleId);
        });
    }

    public void close() {
        isClosed = true;
        if (interpreter != null) {
            interpreter.close();
            Log.d("DetectionProcessor", "Interpreter closed.");
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
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
        long now = System.currentTimeMillis();

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

            Long lastTime = lastPlayedTimePerObject.get(det.objectId);

            if (lastTime == null || (now - lastTime > objectSoundCooldownMs)) {
                pendingSounds.add(new SoundTask(sectorId, det.detectedClass, distance));
                lastPlayedTimePerObject.put(det.objectId, now);
            }

            String className = CocoLabels.getLabel(det.detectedClass);
            if (distance >= 0) {
                resultText.append(String.format(Locale.US,
                        "%s | %.1f%% | Сектор %d | Дистанція: %.2f m\n",
                        className, det.confidence * 100, sectorId, distance));
            } else {
                resultText.append(String.format(Locale.US,
                        "%s | %.1f%% | Сектор %d\n",
                        className, det.confidence * 100, sectorId));
            }
            /*sessionLogger.log("Detected object ID=" + det.objectId +
                    ", Class=" + det.detectedClass +
                    ", Confidence=" + det.confidence);*/
        }

        detectionResultsTextView.post(() -> detectionResultsTextView.setText(resultText.toString()));
        if (!isPlayingSoundPair) playNextSoundPair();
    }

    private void playNextSoundPair() {
        if (pendingSounds.isEmpty()) {
            isPlayingSoundPair = false;
            return;
        }

        isPlayingSoundPair = true;
        SoundTask task = pendingSounds.poll();
        Log.d("SoundPair", "Playing sector " + task.sectorId + " + object " + task.classId);

        playSectorSound(task.sectorId);

        handler.postDelayed(() -> {
            playObjectSound(task.classId, task.distance);
            handler.postDelayed(this::playNextSoundPair, globalSoundPauseMs);
        }, 500); // Delay between sector and object sound
    }

    private static class SoundTask {
        int sectorId, classId;
        float distance;

        SoundTask(int sectorId, int classId, float distance) {
            this.sectorId = sectorId;
            this.classId = classId;
            this.distance = distance;
        }
    }

    private void playSectorSound(int sectorId) {
        String sectorSoundName = SectorSoundManager.getSoundForSector(context, sectorId);
        float quietVolume = 0.05f;

        if (sectorSoundName != null && !sectorSoundName.isEmpty()) {
            if (sectorSoundName.startsWith("content://") || sectorSoundName.startsWith("file://")) {
                try {
                    if (mediaPlayer != null) mediaPlayer.release();
                    mediaPlayer = MediaPlayer.create(context, Uri.parse(sectorSoundName));
                    if (mediaPlayer != null) {
                        mediaPlayer.setVolume(quietVolume, quietVolume);
                        mediaPlayer.setOnCompletionListener(mp -> {
                            mp.release();
                            mediaPlayer = null;
                        });
                        mediaPlayer.start();
                    }
                } catch (Exception e) {
                    Log.e("SOUND", "Failed to play sector URI sound", e);
                }
            } else {
                Integer soundId = sectorSoundMap.get(sectorId);
                if (soundId != null) soundPool.play(soundId, quietVolume, quietVolume, 1, 0, 1f);
            }
        }
    }

    private void playObjectSound(int classId, float distance) {
        String objectName = CocoLabels.getLabel(classId);
        String objectSound = ObjectSoundPreferences.getSoundForObject(context, objectName);
        float volume = DistanceSettingsActivity.getVolumeForDistance(context, distance);

        Log.d("ObjectSound", "Resolved object: " + objectName + ", Sound: " + objectSound + ", Volume: " + volume);

        if (objectSound != null && !objectSound.isEmpty()) {
            if (objectSound.startsWith("content://") || objectSound.startsWith("file://")) {
                try {
                    if (mediaPlayer != null) mediaPlayer.release();
                    mediaPlayer = MediaPlayer.create(context, Uri.parse(objectSound));
                    if (mediaPlayer != null) {
                        mediaPlayer.setVolume(volume, volume);
                        mediaPlayer.setOnCompletionListener(mp -> {
                            mp.release();
                            mediaPlayer = null;
                        });
                        mediaPlayer.start();
                        Log.d("ObjectSound", "Playing URI sound: " + objectSound);
                    } else {
                        Log.e("ObjectSound", "MediaPlayer is null for URI: " + objectSound);
                    }
                } catch (Exception e) {
                    Log.e("ObjectSound", "Failed to play object URI sound", e);
                }
            } else {
                int resId = context.getResources().getIdentifier(objectSound.toLowerCase(), "raw", context.getPackageName());
                Log.d("ObjectSound", "Trying to play raw resource: " + objectSound + ", Res ID: " + resId);
                if (resId != 0) {
                    mediaPlayer = MediaPlayer.create(context, resId);
                    if (mediaPlayer != null) {
                        mediaPlayer.setVolume(volume, volume);
                        mediaPlayer.setOnCompletionListener(mp -> {
                            mp.release();
                            mediaPlayer = null;
                        });
                        mediaPlayer.start();
                        Log.d("ObjectSound", "Played raw sound: " + objectSound);
                    } else {
                        Log.e("ObjectSound", "Failed to create MediaPlayer for raw: " + objectSound);
                    }
                } else {
                    Log.e("ObjectSound", "Invalid raw sound name: " + objectSound);
                }
            }
        } else {
            Log.w("ObjectSound", "No valid sound configured for object: " + objectName);
        }
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

