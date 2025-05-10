package com.example.diplomaappmodeltflite;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

import com.google.android.gms.maps.GoogleMap;
import com.google.android.libraries.navigation.NavigationApi;
import com.google.android.libraries.navigation.Navigator;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraOnlyActivity extends AppCompatActivity {
    private static final String TAG = "CameraOnlyActivity";

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


        /*NavigationApi.getNavigator(this, new NavigationApi.NavigatorListener() {
            @Override
            public void onNavigatorReady(Navigator navigator) {
                SupportNavigationFragment navFragment = (SupportNavigationFragment)
                        getSupportFragmentManager().findFragmentById(R.id.miniNavigationFragment);


                if (navFragment != null) {
                    navFragment.getMapAsync(map -> {
                        if (ContextCompat.checkSelfPermission(CameraOnlyActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {
                            map.followMyLocation(GoogleMap.CameraPerspective.TILTED);
                        } else {
                            showToast("Location permission not granted for camera follow.");
                        }
                    });
                }

                View mapClickOverlay = findViewById(R.id.mapClickOverlay);
                mapClickOverlay.setOnClickListener(v -> {
                    Intent intent = new Intent(CameraOnlyActivity.this, MapOnlyActivity.class);
                    startActivity(intent);
                });
            }

            @Override
            public void onError(@NavigationApi.ErrorCode int errorCode) {
                showToast("Navigation error: " + errorCode);
            }
        });*/

        SupportNavigationFragment navFragment = (SupportNavigationFragment)
                getSupportFragmentManager().findFragmentById(R.id.miniNavigationFragment);

        if (navFragment != null && NavigationSessionManager.getInstance().isNavigationRunning()) {
            Navigator navigator = NavigationSessionManager.getInstance().getNavigator();

            navFragment.getMapAsync(map -> {
                if (ContextCompat.checkSelfPermission(CameraOnlyActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    map.followMyLocation(GoogleMap.CameraPerspective.TILTED);
                } else {
                    showToast("Location permission not granted for camera follow.");
                }
            });

            View mapClickOverlay = findViewById(R.id.mapClickOverlay);
            mapClickOverlay.setOnClickListener(v -> {
                Intent intent = new Intent(CameraOnlyActivity.this, MapOnlyActivity.class);
                startActivity(intent);
            });
        } else {
            showToast("Navigation not running.");
        }

        cameraPreview = findViewById(R.id.cameraPreviewOnly);
        overlayView = findViewById(R.id.overlayViewOnly);
        detectionResultsTextView = findViewById(R.id.detectionResultsTextView);

        overlayView.setModelInputSize(640, 640);
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
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        Log.e(TAG, msg);
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

        cameraPreview.postDelayed(() -> {
            int width = cameraPreview.getWidth();
            int height = cameraPreview.getHeight();
            overlayView.setPreviewSize(width, height);
            Log.d("OverlayView", "Delayed preview size set to: " + width + "x" + height);
        }, 200); // delay in ms


        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(new ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(new ResolutionStrategy(
                                new Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build())
                .build();

        imageAnalysis.setAnalyzer(inferenceExecutor, imageProxy -> {
            if (imageProxy.getImage() != null) {
                int rotation = imageProxy.getImageInfo().getRotationDegrees();
                Bitmap bitmap = CameraUtils.toBitmap(imageProxy.getImage(), rotation);

                // Resize bitmap to model input size
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true);

                // Set model input size for overlay (used for scaling)
                overlayView.setModelInputSize(640, 640);

                // Pass original preview dimensions for overlay scaling
                overlayView.setPreviewSize(cameraPreview.getWidth(), cameraPreview.getHeight());

                // Run detection
                detectionProcessor.process(resizedBitmap);
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

    @Override
    protected void onResume() {
        super.onResume();

        cameraPreview.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int width = cameraPreview.getWidth();
                int height = cameraPreview.getHeight();
                if (width > 0 && height > 0) {
                    overlayView.setPreviewSize(width, height);
                    overlayView.invalidate();
                    Log.d("OverlayView", "GlobalLayout: set preview size to " + width + "x" + height);
                    cameraPreview.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });

        cameraPreview.postDelayed(() -> {
            int width = cameraPreview.getWidth();
            int height = cameraPreview.getHeight();
            overlayView.setPreviewSize(width, height);
            Log.d("OverlayView", "onResume: Re-set preview size to " + width + "x" + height);
        }, 200);
    }

    @Override
    protected void onPause() {
        super.onPause();
        overlayView.setResults(null); // Clear old boxes
        overlayView.invalidate();
    }
}


