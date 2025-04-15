package com.example.diplomaappmodeltflite;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
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
    Location currentLocation;
    private GoogleMap Map;

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat, currentLng;
    private double destLat, destLng;
    private TextView currentLocationTextView;
    private Button startTravelButton;
    private Button btnBack;
    private boolean destinationSelected = false;
    private Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travel);
        Log.d(TAG, "onCreate: TravelActivity started");

        geocoder = new Geocoder(this, Locale.getDefault());


        btnBack = findViewById(R.id.btnBack);

        currentLocationTextView = findViewById(R.id.currentLocationTextView);
        startTravelButton = findViewById(R.id.startTravelButton);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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
                Toast.makeText(this, "Please select a destination.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Start button pressed without destination selected");
                return;
            }
            Log.d(TAG, "Starting CameraActivity with coordinates");
            Intent intent = new Intent(TravelActivity.this, CameraActivity.class);
            intent.putExtra("originLat", currentLat);
            intent.putExtra("originLng", currentLng);
            intent.putExtra("destLat", destLat);
            intent.putExtra("destLng", destLng);
            startActivity(intent);
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

        Task<Location> task = fusedLocationClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null){
                    Log.d(TAG, "Location retrieved: " + location.getLatitude() + ", " + location.getLongitude());

                    currentLocation = location;
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    mapFragment.getMapAsync(TravelActivity.this);

                    String result = null;
                    try{
                        List<Address> addresses =
                                geocoder.getFromLocation(currentLocation.getLatitude(),
                                        currentLocation.getLongitude(), 1);

                        if (addresses != null && !addresses.isEmpty()) {
                            Address address = addresses.get(0);
                            // sending back first address line and locality
                            result = address.getAddressLine(0); // + ", " + address.getLocality()
                            currentLocationTextView.setText(result != null ? result : "Location not available");
                            Log.d(TAG, "Address resolved: " + result);
                        }

                    } catch (IOException e) {
                        Log.e("TravelActivity", "Address error: " + e);
                    }
                    currentLocationTextView.setText(result);

                }
            }
        });
    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap){

        // for testing purposes
        googleMap.setOnCameraIdleListener(() -> {
            Log.d("TravelActivity", "Map camera moved to: " + googleMap.getCameraPosition().target);
        });

        Log.d(TAG, "onMapReady called");
        Map = googleMap;

        LatLng center = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        Map.addMarker(new MarkerOptions().position(center).title("Поточна локація"));
        Map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 20));
    }

    private void setupAutocomplete() {
        AutocompleteSupportFragment autocompleteFragment =
                (AutocompleteSupportFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.autocompleteFragment);

        if (autocompleteFragment != null) {
            autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
            autocompleteFragment.setHint("Destination");

            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    if (place.getLatLng() != null) {
                        destLat = place.getLatLng().latitude;
                        destLng = place.getLatLng().longitude;
                        destinationSelected = true;
                    }
                }

                @Override
                public void onError(@NonNull com.google.android.gms.common.api.Status status) {
                    Log.e("TravelActivity", "Autocomplete error: " + status.getStatusMessage());
                    Toast.makeText(TravelActivity.this, "Error selecting place.", Toast.LENGTH_SHORT).show();
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