package com.example.diplomaappmodeltflite;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private Button btnStartDetection;
    private Button btnSettings;
    private Button sectorsSettingsButton;
    private Button btnHistory;
    private Button objectDistanceSettings;
    private Button objectSoundSettings;
    private Button btnTrip;
    private Button frequencySettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStartDetection = findViewById(R.id.btnStartDetection);
        btnSettings = findViewById(R.id.btnSettings);
        sectorsSettingsButton = findViewById(R.id.sectorsSettingsButton);
        btnHistory = findViewById(R.id.btnHistory);
        objectDistanceSettings = findViewById(R.id.objectDistanceSettings);
        objectSoundSettings = findViewById(R.id.objectSoundSettings);
        btnTrip = findViewById(R.id.btnTrip);
        frequencySettings = findViewById(R.id.frequencySettings);

        btnStartDetection.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
        });

        sectorsSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SectorsSettingsActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AppSettingsActivity.class);
            startActivity(intent);
        });

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        objectDistanceSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DistanceSettingsActivity.class);
            startActivity(intent);
        });

        objectSoundSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ObjectsSettingsActivity.class);
            startActivity(intent);
        });

        btnTrip.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TravelActivity.class);
            startActivity(intent);
        });

        frequencySettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FrequencyActivity.class);
            startActivity(intent);
        });

    }
}
