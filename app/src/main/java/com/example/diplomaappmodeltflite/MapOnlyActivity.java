package com.example.diplomaappmodeltflite;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
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
import java.util.Locale;

public class MapOnlyActivity extends AppCompatActivity {
    private static final String TAG = "MapOnlyActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 1010;

    private Navigator navigator;
    private SupportNavigationFragment navFragment;
    private RoutingOptions routingOptions;
    private boolean locationPermissionGranted;
    private CompassManager compassManager;
    private SessionLogger sessionLogger;
    private double startLat, startLng, destLat, destLng;
    private String startLocationName, endLocationName;
    private long startTime;
    private double tripDistance;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_only);

        compassManager = new CompassManager(this);

        // Get data passed from TravelActivity
        startLocationName = getIntent().getStringExtra("startLocationName");
        endLocationName = getIntent().getStringExtra("endLocationName");
        tripDistance = getIntent().getDoubleExtra("tripDistanceKm", 0.0);

        sessionLogger = new SessionLogger(this);
        sessionLogger.setStartLocation(startLocationName);
        sessionLogger.setEndLocation(endLocationName);
        sessionLogger.setDistanceKm(tripDistance);

        initializeNavigationSdk();
    }


    private void initializeNavigationSdk() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }

        if (!locationPermissionGranted) {
            showToast("Location permission is required.");
            return;
        }

        NavigationApi.getNavigator(this, new NavigationApi.NavigatorListener() {
            @Override
            public void onNavigatorReady(Navigator nav) {
                navigator = nav;

                navFragment = (SupportNavigationFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fullMap);

                if (navFragment == null) {
                    showToast("Navigation Fragment is null");
                    return;
                }

                View compassButton = getLayoutInflater().inflate(R.layout.compass_button, null);
                View stopButton = getLayoutInflater().inflate(R.layout.stop_button, null);
                View backBtn = getLayoutInflater().inflate(R.layout.back_button, null);

                navFragment.setCustomControl(compassButton, CustomControlPosition.SECONDARY_HEADER);
                navFragment.setCustomControl(stopButton, CustomControlPosition.FOOTER);
                navFragment.setCustomControl(backBtn, CustomControlPosition.BOTTOM_START_BELOW);

                backBtn.setOnClickListener(v -> {
                    Intent intent = new Intent(MapOnlyActivity.this, CameraActivity.class);
                    startActivity(intent);
                });

                compassButton.setOnClickListener(v -> compassManager.startListening());

                stopButton.setOnClickListener(v -> {
                    if (navigator != null) {
                        navigator.stopGuidance();
                        navigator.cleanup();
                    }

                    long durationMillis = SystemClock.elapsedRealtime() - startTime;
                    String durationFormatted = formatDuration(durationMillis);

                    sessionLogger.setDuration(durationFormatted);

                    captureMapSnapshot();

                    showToast("Навігацію зупинено");
                });

                backBtn.setOnClickListener(v -> {
                    startActivity(new Intent(MapOnlyActivity.this, CameraActivity.class));
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

                routingOptions = new RoutingOptions();
                routingOptions.travelMode(RoutingOptions.TravelMode.DRIVING);

                double destLat = getIntent().getDoubleExtra("destLat", 0);
                double destLng = getIntent().getDoubleExtra("destLng", 0);

                if (destLat == 0 && destLng == 0) {
                    //showToast("Destination not set.");
                    return;
                }

                try {
                    Waypoint destination = Waypoint.builder()
                            .setLatLng(destLat, destLng)
                            .build();

                    startNavigation(destination, routingOptions);

                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException during navigation: " + e.getMessage());
                    showToast("Navigation failed: location permission missing");
                }
            }

            @Override
            public void onError(@NavigationApi.ErrorCode int errorCode) {
                showToast("Navigation SDK error: " + errorCode);
            }
        });
    }

    private void startNavigation(Waypoint destination, RoutingOptions options) {
        ListenableResultFuture<Navigator.RouteStatus> pendingRoute = navigator.setDestination(destination, options);

        pendingRoute.setOnResultListener(routeStatus -> {
            if (routeStatus == Navigator.RouteStatus.OK) {

                navigator.setAudioGuidance(Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE);

                if (BuildConfig.DEBUG) {
                    navigator.getSimulator().simulateLocationsAlongExistingRoute(
                            new SimulationOptions().speedMultiplier(1));
                }

                navigator.startGuidance();
            } else {
                showToast("Navigation failed: " + routeStatus);
            }
        });
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
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                initializeNavigationSdk();
            } else {
                showToast("Location permission is required.");
            }
        }
    }
}

