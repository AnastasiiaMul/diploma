package com.example.diplomaappmodeltflite;

import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Size;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionStrategy;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraOnlyActivity extends AppCompatActivity {

    private PreviewView cameraPreview;
    private OverlayView overlayView;
    private TextView detectionResultsTextView;
    private DetectionProcessor detectionProcessor;
    private SoundPool soundPool;
    private Map<Integer, Integer> sectorSoundMap = new HashMap<>();
    private SessionLogger sessionLogger;

    private ExecutorService inferenceExecutor;
    private ObjectDetectorHelper objectDetectorHelper;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_only);

        cameraPreview = findViewById(R.id.cameraPreviewOnly);
        overlayView = findViewById(R.id.overlayViewOnly);
        detectionResultsTextView = findViewById(R.id.detectionResultsTextView);

        Button backBtn = findViewById(R.id.btnBack);
        backBtn.setOnClickListener(v -> finish());

        sessionLogger = new SessionLogger(this);
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

        detectionProcessor = new DetectionProcessor(
                this,
                objectDetectorHelper,
                overlayView,
                detectionResultsTextView,
                soundPool,
                sectorSoundMap,
                sessionLogger
        );

        inferenceExecutor = Executors.newSingleThreadExecutor();

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
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

        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(new ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(new ResolutionStrategy(
                                new Size(640, 640),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build())
                .build();

        imageAnalysis.setAnalyzer(inferenceExecutor, imageProxy -> {
            if (imageProxy.getImage() != null) {
                detectionProcessor.process(CameraUtils.toBitmap(imageProxy.getImage()));
            }
            imageProxy.close();
        });

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) soundPool.release();
        if (inferenceExecutor != null) inferenceExecutor.shutdown();
        if (objectDetectorHelper != null) objectDetectorHelper.close();
    }
}


