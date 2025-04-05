package com.example.diplomaappmodeltflite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnStartDetection;
    private Button btnSettings;
    private Button sectorsSettingsButton;
    private Button btnHistory;
    private Button objectDistanceSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStartDetection = findViewById(R.id.btnStartDetection);
        btnSettings = findViewById(R.id.btnSettings);
        sectorsSettingsButton = findViewById(R.id.sectorsSettingsButton);
        btnHistory = findViewById(R.id.btnHistory);
        objectDistanceSettings = findViewById(R.id.objectDistanceSettings);

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
    }
}
