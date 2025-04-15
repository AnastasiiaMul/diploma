package com.example.diplomaappmodeltflite;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.fragment.app.Fragment;

import android.media.AudioAttributes;
import android.media.Image;
import android.media.SoundPool;
import org.tensorflow.lite.Interpreter;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private OverlayView overlayView;
    private TextView detectionResultsTextView;
    private TextView fpsTextView;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService inferenceExecutor;

    private SoundPool soundPool;
    private final Map<Integer, Integer> sectorSoundMap = new HashMap<>();

    private ObjectDetectorHelper objectDetectorHelper;
    private DetectionProcessor detectionProcessor;
    private SessionLogger sessionLogger;

    private long lastFrameTime = 0;
    private int frameCounter = 0;

    private static final int FRAME_SKIP_INTERVAL = 5;

    // data to approximate the distance
    // Constants for object heights (in meters)


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.cameraPreview);

        overlayView = findViewById(R.id.overlayView);

        detectionResultsTextView = findViewById(R.id.detectionResultsTextView);
        fpsTextView = findViewById(R.id.fpsTextView);

        sessionLogger = new SessionLogger(this);

        inferenceExecutor = Executors.newSingleThreadExecutor();

        previewView.setOnClickListener(v -> startActivity(new Intent(this, CameraOnlyActivity.class)));

        View mapClickOverlay = findViewById(R.id.mapClickOverlay);
        mapClickOverlay.setOnClickListener(v -> {
            Intent intent = new Intent(CameraActivity.this, MapOnlyActivity.class);
            startActivity(intent);
        });

        objectDetectorHelper = new ObjectDetectorHelper(this);

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();

        int sectorCount = SectorSoundManager.getNumberOfSectors(this);
        for (int sectorId = 1; sectorId <= sectorCount; sectorId++) {
            String soundName = SectorSoundManager.getSoundForSector(this, sectorId);
            if (!soundName.isEmpty()) {
                int resId = getResources().getIdentifier(soundName, "raw", getPackageName());
                if (resId != 0) {
                    int soundId = soundPool.load(this, resId, 1);
                    sectorSoundMap.put(sectorId, soundId);
                }
            }
        }

        detectionProcessor = new DetectionProcessor(this, objectDetectorHelper, overlayView,
                detectionResultsTextView, soundPool, sectorSoundMap, sessionLogger);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCamera(cameraProvider);
            } catch (Exception e) {
                Log.e("CameraX", "Error binding camera", e);
            }
        }, ContextCompat.getMainExecutor(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void bindCamera(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .setResolutionSelector(new ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(new ResolutionStrategy(
                                new Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build())
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(new ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(new ResolutionStrategy(
                                new Size(640, 640),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build())
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::analyzeImage);

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeImage(ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image == null) {
            imageProxy.close();
            return;
        }

        frameCounter++;
        if (frameCounter % FRAME_SKIP_INTERVAL == 0) {
            Bitmap bitmap = CameraUtils.toBitmap(image);
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true);
            detectionProcessor.process(resizedBitmap);
        }

        imageProxy.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (detectionProcessor != null) detectionProcessor.close();
        if (objectDetectorHelper != null) objectDetectorHelper.close();
        if (inferenceExecutor != null) inferenceExecutor.shutdown();
        soundPool.release();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 1001);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 &&
                (grantResults.length == 0 || grantResults[0] != android.content.pm.PackageManager.PERMISSION_GRANTED)) {
            finish(); // Close if permission is denied
        }
    }

    private void rebindCameraIfNeeded() {
        if (cameraProviderFuture != null) {
            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindCamera(cameraProvider);
                } catch (Exception e) {
                    Log.e("CameraX", "Error rebinding camera", e);
                }
            }, ContextCompat.getMainExecutor(this));
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Recreate detector and processor (because interpreter might've been closed)
        objectDetectorHelper = new ObjectDetectorHelper(this);
        detectionProcessor = new DetectionProcessor(
                this,
                objectDetectorHelper,
                overlayView,
                detectionResultsTextView,
                soundPool,
                sectorSoundMap,
                sessionLogger
        );

        rebindCameraIfNeeded();
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (objectDetectorHelper != null) {
            objectDetectorHelper.close();
            objectDetectorHelper = null;
        }
    }




}