package com.example.diplomaappmodeltflite;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
import com.google.android.libraries.navigation.NavigationApi;
import com.google.android.libraries.navigation.Navigator;
import com.google.android.libraries.navigation.RoutingOptions;
import com.google.android.libraries.navigation.Waypoint;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TravelActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "TravelActivity";

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private GoogleMap map;
    private Location currentLocation;
    private double currentLat, currentLng;
    private String startLocationName, endLocationName;
    private double destLat, destLng;
    private boolean destinationSelected = false;

    private FusedLocationProviderClient fusedLocationClient;
    private Geocoder geocoder;

    private TextView currentLocationTextView;
    private Button startTravelButton, btnBack;
    private SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travel);

        geocoder = new Geocoder(this, Locale.getDefault());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        btnBack = findViewById(R.id.btnBack);
        currentLocationTextView = findViewById(R.id.currentLocationTextView);
        startTravelButton = findViewById(R.id.startTravelButton);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        // Initialize Places SDK
        if (!Places.isInitialized()) {
            String apiKey = getMetaDataApiKey();
            if (apiKey != null) {
                Log.d(TAG, "Initializing Places SDK");
                Places.initialize(getApplicationContext(), apiKey);
            }else {
                Log.e(TAG, "API key is null");
            }

        }

        requestLocation();
        setupAutocomplete();

        startTravelButton.setOnClickListener(v -> {
            if (!destinationSelected) {
                Toast.makeText(this, "Будь ласка, оберіть місце призначення.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Start button pressed without destination selected");
                return;
            }
            Log.d(TAG, "Saving travel data and starting MapOnlyActivity");

            NavigationSessionManager.getInstance().startNavigation(TravelActivity.this, new NavigationApi.NavigatorListener() {
                @Override
                public void onNavigatorReady(Navigator navigator) {
                    Log.d(TAG, "Navigator ready in TravelActivity");

                    try {
                        // Set destination
                        Waypoint destination = Waypoint.builder()
                                .setLatLng(destLat, destLng)
                                .build();
                        Log.d(TAG, "set destination" + destLng + " " + destLat);

                        navigator.setAudioGuidance(Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE);
                        navigator.setDestination(destination, new RoutingOptions())
                                .setOnResultListener(routeStatus -> {
                                    if (routeStatus == Navigator.RouteStatus.OK) {
                                        Log.d(TAG, "Route calculated successfully, starting guidance...");
                                        navigator.startGuidance();

                                        Intent intent = new Intent(TravelActivity.this, MapOnlyActivity.class);
                                        intent.putExtra("originLat", currentLat);
                                        intent.putExtra("originLng", currentLng);
                                        intent.putExtra("startLocationName", startLocationName);
                                        intent.putExtra("destLat", destLat);
                                        intent.putExtra("destLng", destLng);
                                        intent.putExtra("endLocationName", endLocationName);
                                        intent.putExtra("startTimeMillis", System.currentTimeMillis());
                                        startActivity(intent);
                                    } else {
                                        Log.e(TAG, "Failed to calculate route: " + routeStatus);
                                        Toast.makeText(TravelActivity.this, "Не вдалося розрахувати маршрут.", Toast.LENGTH_SHORT).show();
                                    }
                                });

                    } catch (Exception e) {
                        Log.e(TAG, "Exception while setting destination", e);
                        Toast.makeText(TravelActivity.this, "Помилка навігації.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(@NavigationApi.ErrorCode int errorCode) {
                    Log.e(TAG, "Failed to start navigator: " + errorCode);
                    Toast.makeText(TravelActivity.this, "Помилка запуску навігації.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting location permission");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }
        Log.d(TAG, "Location permissions granted, getting last known location");

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation = location;
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
                if (mapFragment != null) mapFragment.getMapAsync(this);

                try {
                    List<Address> addresses = geocoder.getFromLocation(currentLat, currentLng, 1);
                    if (!addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        startLocationName = address.getAddressLine(0);
                        currentLocationTextView.setText(startLocationName);
                    }
                } catch (IOException e) {
                    currentLocationTextView.setText("Невдалось визначити адресу");
                }
            }
        });
    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap){
        map = googleMap;

        if (currentLocation != null) {
            LatLng center = new LatLng(currentLat, currentLng);
            map.addMarker(new MarkerOptions().position(center).title("Поточна локація"));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 17));
        }
    }

    private void setupAutocomplete() {
        AutocompleteSupportFragment autocompleteFragment =
                (AutocompleteSupportFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.autocompleteFragment);

        if (autocompleteFragment != null) {
            autocompleteFragment.setPlaceFields(Arrays.asList(
                    Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS));
            autocompleteFragment.setHint("Місце призначення");

            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    if (place.getLatLng() != null) {
                        destLat = place.getLatLng().latitude;
                        destLng = place.getLatLng().longitude;
                        if (place.getFormattedAddress() != null) {
                            endLocationName = place.getFormattedAddress();
                        } else {
                            endLocationName = place.getName();
                        }
                        destinationSelected = true;
                    }
                }

                @Override
                public void onError(@NonNull com.google.android.gms.common.api.Status status) {
                    Log.e("TravelActivity", "Autocomplete error: " + status.getStatusMessage());
                    Toast.makeText(TravelActivity.this, "Помилка вибору місця", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Handle location permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocation();
            } else {
                Toast.makeText(this, "Location permission is required.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getMetaDataApiKey() {
        try {
            Bundle metaData = getPackageManager()
                    .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA)
                    .metaData;
            return metaData.getString("com.google.android.geo.API_KEY");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

}