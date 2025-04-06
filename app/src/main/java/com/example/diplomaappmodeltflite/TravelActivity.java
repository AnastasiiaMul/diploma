package com.example.diplomaappmodeltflite;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.util.Arrays;

public class TravelActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat, currentLng;
    private double destLat, destLng;
    private TextView currentLocationTextView;
    private Button startTravelButton;
    private Button btnBack;

    private boolean destinationSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travel);

        btnBack = findViewById(R.id.btnBack);

        currentLocationTextView = findViewById(R.id.currentLocationTextView);
        startTravelButton = findViewById(R.id.startTravelButton);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize Places SDK
        if (!Places.isInitialized()) {
            String apiKey = getMetaDataApiKey();
            if (apiKey != null && !Places.isInitialized()) {
                Places.initialize(getApplicationContext(), apiKey);
            }

        }

        requestLocation();

        setupAutocomplete();

        startTravelButton.setOnClickListener(v -> {
            if (!destinationSelected) {
                Toast.makeText(this, "Please select a destination.", Toast.LENGTH_SHORT).show();
                return;
            }

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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
                currentLocationTextView.setText(String.format("Lat: %.5f, Lng: %.5f", currentLat, currentLng));
            } else {
                currentLocationTextView.setText("Location not available.");
            }
        });
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

