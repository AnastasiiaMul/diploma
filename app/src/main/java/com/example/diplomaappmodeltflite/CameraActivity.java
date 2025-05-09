package com.example.diplomaappmodeltflite;

import android.annotation.SuppressLint;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.Image;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
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
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;


public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private OverlayView overlayView;
    private TextView detectionResultsTextView;
    private TextView fpsTextView;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService inferenceExecutor;

    private SoundPool soundPool;
    private final Map<Integer, Integer> sectorSoundMap = new HashMap<>();
    private final Map<Integer, Boolean> loadedSoundFlags = new HashMap<>();
    private ObjectDetectorHelper objectDetectorHelper;
    private DetectionProcessor detectionProcessor;
    private SessionLogger sessionLogger;
    private CompassManager compassManager;

    private long lastFrameTime = 0;
    private int frameCounter = 0;

    private static final int FRAME_SKIP_INTERVAL = 5;

    // data to approximate the distance
    // Constants for object heights (in meters)

    private TextView navigationInfoTextView, azimuthTextView;
    private FusedLocationProviderClient fusedLocationClient;
    private Geocoder geocoder;

    private double originLat, originLng;
    private double destLat, destLng;
    private String startLocationName, endLocationName;
    private boolean navigationStarted = false;

    private float currentAzimuth = 0f;
    private float targetBearing = 0f;

    private long lastCompassUpdate = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        SharedPreferences prefs = getSharedPreferences("FrequencyPrefs", MODE_PRIVATE);
        int compassUpdateInterval = prefs.getInt("compass_update_interval_ms", 5000);
        boolean updateOnPress = prefs.getBoolean("compass_update_on_press", false);

        previewView = findViewById(R.id.cameraPreview);
        overlayView = findViewById(R.id.overlayView);
        detectionResultsTextView = findViewById(R.id.detectionResultsTextView);

        // Get real screen size
        /*DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        overlayView.setPreviewSize(displayMetrics.widthPixels, displayMetrics.heightPixels);*/

        navigationInfoTextView = findViewById(R.id.navigationInfoTextView);
        azimuthTextView = findViewById(R.id.azimuthTextView);

        if (updateOnPress) {
            azimuthTextView.setOnClickListener(v -> updateAzimuthDisplay());
        } else {
            azimuthTextView.setOnClickListener(null); // disable listener in case it was set before
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(this, Locale.getDefault());
        // Fetch and show location
        loadTravelSession();
        requestCurrentLocation();

        compassManager = new CompassManager(this);
        if(!updateOnPress)
            compassManager.setCompassListener(azimuth -> {
                currentAzimuth = azimuth;

                if (!updateOnPress) {
                    long now = System.currentTimeMillis();
                    if (now - lastCompassUpdate >= compassUpdateInterval) {
                        updateAzimuthDisplay();
                        lastCompassUpdate = now;
                    }
                }
            });

        compassManager.startListening();

        fpsTextView = findViewById(R.id.fpsTextView);

        sessionLogger = new SessionLogger(this);

        inferenceExecutor = Executors.newSingleThreadExecutor();

        previewView.setOnClickListener(v -> startActivity(new Intent(this, CameraOnlyActivity.class)));

        objectDetectorHelper = new ObjectDetectorHelper(this);

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();

        soundPool.setOnLoadCompleteListener((soundPool1, sampleId, status) -> {
            if (status == 0) { // 0 means success
                for (Map.Entry<Integer, Integer> entry : sectorSoundMap.entrySet()) {
                    if (entry.getValue() == sampleId) {
                        loadedSoundFlags.put(entry.getKey(), true);
                        Log.d("SoundPool", "Loaded sector sound for sector " + entry.getKey());
                        break;
                    }
                }
            } else {
                Log.e("SoundPool", "Failed to load sound ID " + sampleId);
            }
        });

        int sectorCount = SectorSoundManager.getNumberOfSectors(this);
        for (int sectorId = 1; sectorId <= sectorCount; sectorId++) {
            String soundName = SectorSoundManager.getSoundForSector(this, sectorId);
            if (!soundName.isEmpty() && !soundName.startsWith("content://") && !soundName.startsWith("file://")) {
                int resId = getResources().getIdentifier(soundName, "raw", getPackageName());
                if (resId != 0) {
                    int soundId = soundPool.load(this, resId, 1);
                    sectorSoundMap.put(sectorId, soundId);
                    loadedSoundFlags.put(sectorId, false); // not ready yet
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

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            Intent intent = new Intent(CameraActivity.this, MainActivity.class);
            startActivity(intent);
        });
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

        // Set preview size to OverlayView
        previewView.post(() -> {
            overlayView.setPreviewSize(previewView.getWidth(), previewView.getHeight());
        });

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

    private void loadTravelSession() {
        SharedPreferences prefs = getSharedPreferences("TravelSession", MODE_PRIVATE);

        originLat = prefs.getFloat("originLat", 0);
        originLng = prefs.getFloat("originLng", 0);
        startLocationName = prefs.getString("startLocationName", "Поточна локація");
        destLat = prefs.getFloat("destLat", 0);
        destLng = prefs.getFloat("destLng", 0);
        endLocationName = prefs.getString("endLocationName", "Пункт призначення");

        navigationStarted = (destLat != 0 && destLng != 0); // Only if destination was properly set

        Log.d("CameraActivity", "Travel session loaded: " + startLocationName + " -> " + endLocationName);
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

    private void requestCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                if (navigationStarted) {
                    Location dest = new Location("");
                    dest.setLatitude(destLat);
                    dest.setLongitude(destLng);
                    targetBearing = location.bearingTo(dest);
                    if (targetBearing < 0) targetBearing += 360;
                }
                updateFullNavigationInfo(location);
                updateAzimuthDisplay();
            }
        });
    }
    private void updateAzimuthDisplay() {
        runOnUiThread(() -> {
            String text = String.format(Locale.US, "Кут руху: %.1f°\nЦільовий кут: %.1f°", currentAzimuth, targetBearing);
            azimuthTextView.setText(text);
        });
    }




    private void updateFullNavigationInfo(Location currentLocation) {
        StringBuilder info = new StringBuilder();

        // --- Part 1: Current Location ---
        try {
            List<Address> addresses = geocoder.getFromLocation(currentLocation.getLatitude(), currentLocation.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                info.append("Поточна локація:\n").append(address.getAddressLine(0));
            } else {
                info.append("Поточна локація: Невідома");
            }
        } catch (Exception e) {
            info.append("Поточна локація: Помилка");
        }

        // --- Part 2: Start and Destination (only if navigation started) ---
        if (navigationStarted) {
            info.append("\n\nПункт відправлення: ").append(startLocationName != null ? startLocationName : "Невідомо");
            info.append("\nПункт призначення: ").append(endLocationName != null ? endLocationName : "Невідомо");

            float[] result = new float[1];
            Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), destLat, destLng, result);
            float distanceInMeters = result[0];
            info.append(String.format(Locale.US, "\nВідстань до цілі: %.2f м", distanceInMeters));
        } else {
            info.append("\n\nНавігація не запущена.");
        }

        navigationInfoTextView.setText(info.toString());
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
        loadTravelSession();

        if (compassManager != null) {
            compassManager.startListening();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (objectDetectorHelper != null) {
            objectDetectorHelper.close();
            objectDetectorHelper = null;
        }

        if (compassManager != null) {
            compassManager.stopListening();
        }
    }
}