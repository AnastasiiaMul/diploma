package com.example.diplomaappmodeltflite;


import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.libraries.navigation.CustomControlPosition;
import com.google.android.libraries.navigation.ListenableResultFuture;
import com.google.android.libraries.navigation.NavigationApi;
import com.google.android.libraries.navigation.Navigator;
import com.google.android.libraries.navigation.RoutingOptions;
import com.google.android.libraries.navigation.SimulationOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.android.libraries.navigation.Waypoint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapOnlyActivity extends AppCompatActivity {
    private static final String TAG = "MapOnlyActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 1010;

    private Navigator navigator;
    private SupportNavigationFragment navFragment;
    private CompassManager compassManager;
    private SessionLogger sessionLogger;
    private FusedLocationProviderClient fusedLocationClient;

    private double startLat, startLng, destLat, destLng;
    private String startLocationName, endLocationName;
    private long startTimeMillis, startElapsedTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_only);

        compassManager = new CompassManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get data passed from TravelActivity
        Intent intent = getIntent();
        startLat = intent.getDoubleExtra("originLat", 0);
        startLng = intent.getDoubleExtra("originLng", 0);
        destLat = intent.getDoubleExtra("destLat", 0);
        destLng = intent.getDoubleExtra("destLng", 0);
        startLocationName = intent.getStringExtra("startLocationName");
        endLocationName = intent.getStringExtra("endLocationName");
        startTimeMillis = intent.getLongExtra("startTimeMillis", System.currentTimeMillis());
        startElapsedTime = intent.getLongExtra("startElapsedTime", SystemClock.elapsedRealtime());

        sessionLogger = new SessionLogger(this);
        sessionLogger.setStartLocation(startLocationName);
        sessionLogger.setEndLocation(endLocationName);
        sessionLogger.setStartTimeMillis(startTimeMillis);
        sessionLogger.setStartCoordinates(startLat, startLng);

        // Save navigation session for CameraActivity
        SharedPreferences.Editor editor = getSharedPreferences("TravelSession", MODE_PRIVATE).edit();
        editor.putFloat("originLat", (float) startLat);
        editor.putFloat("originLng", (float) startLng);
        editor.putFloat("destLat", (float) destLat);
        editor.putFloat("destLng", (float) destLng);
        editor.putString("startLocationName", startLocationName);
        editor.putString("endLocationName", endLocationName);
        editor.apply();


        initializeNavigationSdk();
    }


    private void initializeNavigationSdk() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        NavigationSessionManager.getInstance().startNavigation(this, new NavigationApi.NavigatorListener() {
            @Override
            public void onNavigatorReady(Navigator nav) {
                navigator = nav;
                setupNavigationUI();
            }

            @Override
            public void onError(@NavigationApi.ErrorCode int errorCode) {
                showToast("Navigation SDK error: " + errorCode);
            }
        });
    }
    private void setupNavigationUI() {
        navFragment = (SupportNavigationFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fullMap);

        if (navFragment == null) {
            showToast("Navigation Fragment is null");
            return;
        }

        View compassButton = getLayoutInflater().inflate(R.layout.compass_button, null);
        View stopButton = getLayoutInflater().inflate(R.layout.stop_button, null);
        View backButton = getLayoutInflater().inflate(R.layout.back_button, null);

        navFragment.setCustomControl(compassButton, CustomControlPosition.SECONDARY_HEADER);
        navFragment.setCustomControl(stopButton, CustomControlPosition.FOOTER);
        navFragment.setCustomControl(backButton, CustomControlPosition.BOTTOM_START_BELOW);

        compassButton.setOnClickListener(v -> compassManager.startListening());

        stopButton.setOnClickListener(v -> stopNavigation());

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(MapOnlyActivity.this, CameraActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();

        });

        navFragment.getMapAsync(map -> {
            if (ContextCompat.checkSelfPermission(MapOnlyActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                map.followMyLocation(GoogleMap.CameraPerspective.TILTED);
            } else {
                showToast("Location permission not granted for camera follow.");
            }
        });

        if (!NavigationSessionManager.getInstance().isNavigationRunning()) {
            startRouteNavigation();
        }else {
            Log.d(TAG, "Navigation already running — skipping start.");
        }
    }

    private void startRouteNavigation() {
        if (destLat == 0 && destLng == 0) {
            showToast("Destination coordinates are missing.");
            return;
        }

        Waypoint destination = Waypoint.builder()
                .setLatLng(destLat, destLng)
                .build();

        RoutingOptions routingOptions = new RoutingOptions().travelMode(RoutingOptions.TravelMode.WALKING);

        navigator.setDestination(destination, routingOptions)
                .setOnResultListener(status -> {
                    if (status == Navigator.RouteStatus.OK) {
                        navigator.setAudioGuidance(Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE);
                        if (BuildConfig.DEBUG) {
                            navigator.getSimulator().simulateLocationsAlongExistingRoute(
                                    new SimulationOptions().speedMultiplier(1));
                        }
                        navigator.startGuidance();
                    } else {
                        showToast("Failed to calculate route: " + status);
                    }
                });
    }

    private void stopNavigation() {
        NavigationSessionManager.getInstance().stopNavigation();

        long durationMillis = SystemClock.elapsedRealtime() - startElapsedTime;
        sessionLogger.setDuration(formatDuration(durationMillis));
        sessionLogger.setEndTimeMillis(System.currentTimeMillis());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    sessionLogger.setEndCoordinates(location.getLatitude(), location.getLongitude());

                    try {
                        List<Address> addresses = new Geocoder(this, Locale.getDefault())
                                .getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        if (!addresses.isEmpty()) {
                            sessionLogger.setEndAddress(addresses.get(0).getAddressLine(0));
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to resolve end address", e);
                    }
                }

                captureMapSnapshot();
                showToast("Навігацію зупинено");
                finish();
            });
        } else {
            captureMapSnapshot();
            showToast("Навігацію зупинено (без локації)");
            finish();
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        Log.e(TAG, msg);
    }

    private void captureMapSnapshot() {
        if (navFragment == null) return;

        navFragment.getMapAsync(map -> map.snapshot(bitmap -> {
            try {
                File snapshotDir = new File(getFilesDir(), "snapshots");
                if (!snapshotDir.exists()) snapshotDir.mkdirs();

                String filename = "snapshot_" + new Date().getTime() + ".png";
                File snapshotFile = new File(snapshotDir, filename);

                try (FileOutputStream out = new FileOutputStream(snapshotFile)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    sessionLogger.setSnapshotPath(snapshotFile.getAbsolutePath());
                    sessionLogger.finalizeLog();
                    Log.d(TAG, "Snapshot saved: " + snapshotFile.getAbsolutePath());
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to save snapshot", e);
            }
        }));
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeNavigationSdk();
        } else {
            showToast("Location permission is required.");
        }
    }
}