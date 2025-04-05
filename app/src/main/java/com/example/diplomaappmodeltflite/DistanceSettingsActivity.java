package com.example.diplomaappmodeltflite;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DistanceSettingsActivity extends AppCompatActivity {

    private EditText minDistanceInput, maxDistanceInput, minVolumeInput, maxVolumeInput, numGradationsInput;
    private Button saveButton, backButton;

    private static final String PREFS_NAME = "distance_settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distance_settings);

        minDistanceInput = findViewById(R.id.minDistanceInput);
        maxDistanceInput = findViewById(R.id.maxDistanceInput);
        minVolumeInput = findViewById(R.id.minVolumeInput);
        maxVolumeInput = findViewById(R.id.maxVolumeInput);
        numGradationsInput = findViewById(R.id.gradationsInput);

        saveButton = findViewById(R.id.saveDistanceSettingsButton);
        backButton = findViewById(R.id.backButton);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load existing settings
        minDistanceInput.setText(String.valueOf(prefs.getFloat("minDistance", 0f)));
        maxDistanceInput.setText(String.valueOf(prefs.getFloat("maxDistance", 10f)));
        minVolumeInput.setText(String.valueOf(prefs.getFloat("minVolume", 0f)));
        maxVolumeInput.setText(String.valueOf(prefs.getFloat("maxVolume", 1f)));
        numGradationsInput.setText(String.valueOf(prefs.getInt("numGradations", 5)));

        saveButton.setOnClickListener(v -> {
            try {
                float minDistance = Float.parseFloat(minDistanceInput.getText().toString());
                float maxDistance = Float.parseFloat(maxDistanceInput.getText().toString());
                float minVolume = Float.parseFloat(minVolumeInput.getText().toString());
                float maxVolume = Float.parseFloat(maxVolumeInput.getText().toString());
                int numGradations = Integer.parseInt(numGradationsInput.getText().toString());

                prefs.edit()
                        .putFloat("minDistance", minDistance)
                        .putFloat("maxDistance", maxDistance)
                        .putFloat("minVolume", minVolume)
                        .putFloat("maxVolume", maxVolume)
                        .putInt("numGradations", numGradations)
                        .apply();

                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
                finish();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
            }
        });

        backButton.setOnClickListener(v -> finish());
    }

    /**
     * Public static method to compute volume based on distance and saved preferences.
     */
    public static float getVolumeForDistance(Context context, float distance) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        float minDistance = prefs.getFloat("minDistance", 0f);
        float maxDistance = prefs.getFloat("maxDistance", 10f);
        float minVolume = prefs.getFloat("minVolume", 0f);
        float maxVolume = prefs.getFloat("maxVolume", 1f);
        int gradations = prefs.getInt("numGradations", 5);

        if (distance < minDistance) {
            return maxVolume;
        } else if (distance > maxDistance) {
            return minVolume;
        }

        float stepSize = (maxDistance - minDistance) / gradations;
        float volumeStep = (maxVolume - minVolume) / gradations;

        int level = (int) ((distance - minDistance) / stepSize);
        float adjustedVolume = maxVolume - (level * volumeStep);

        return Math.max(minVolume, Math.min(adjustedVolume, maxVolume));
    }
}