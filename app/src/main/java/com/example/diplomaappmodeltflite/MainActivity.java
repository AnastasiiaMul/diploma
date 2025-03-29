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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStartDetection = findViewById(R.id.btnStartDetection);
        btnSettings = findViewById(R.id.btnSettings);

        btnStartDetection.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.sectorsSettingsButton).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SectorsSettingsActivity.class);
            startActivity(intent);
        });


        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AppSettingsActivity.class);
            startActivity(intent);
        });;
    }
}
