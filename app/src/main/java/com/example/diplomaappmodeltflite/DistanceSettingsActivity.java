package com.example.diplomaappmodeltflite;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class DistanceSettingsActivity extends AppCompatActivity {

    private EditText minDistanceInput, maxDistanceInput, minVolumeInput, maxVolumeInput, numGradationsInput;
    private Button saveButton, backButton;
    private TextView stepSizeInfoTextView;


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

        stepSizeInfoTextView = findViewById(R.id.stepSizeInfoTextView);
        updateStepSizeText();  // calculate and show initial step size


        saveButton = findViewById(R.id.saveDistanceSettingsButton);
        backButton = findViewById(R.id.backButton);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load existing settings
        minDistanceInput.setText(String.valueOf(prefs.getFloat("minDistance", 0f)));
        maxDistanceInput.setText(String.valueOf(prefs.getFloat("maxDistance", 10f)));
        minVolumeInput.setText(String.valueOf(prefs.getFloat("minVolume", 0f)));
        maxVolumeInput.setText(String.valueOf(prefs.getFloat("maxVolume", 1f)));
        numGradationsInput.setText(String.valueOf(prefs.getInt("numGradations", 5)));

        TextWatcher watcher = new DistanceTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateStepSizeText();
            }
        };

        minDistanceInput.addTextChangedListener(watcher);
        maxDistanceInput.addTextChangedListener(watcher);
        numGradationsInput.addTextChangedListener(watcher);


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

                Toast.makeText(this, "Налаштування збережено", Toast.LENGTH_SHORT).show();
                finish();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
            }
        });

        backButton.setOnClickListener(v -> finish());
    }

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

    private void updateStepSizeText() {
        try {
            float minDistance = Float.parseFloat(minDistanceInput.getText().toString());
            float maxDistance = Float.parseFloat(maxDistanceInput.getText().toString());
            int gradations = Integer.parseInt(numGradationsInput.getText().toString());

            if (gradations > 0 && maxDistance > minDistance) {
                float stepSize = (maxDistance - minDistance) / gradations;
                stepSizeInfoTextView.setText(String.format(Locale.getDefault(),
                        "Крок відстані: %.2f м", stepSize));
            } else {
                stepSizeInfoTextView.setText("Крок відстані: –");
            }
        } catch (NumberFormatException e) {
            stepSizeInfoTextView.setText("Крок відстані: –");
        }
    }
    private static abstract class DistanceTextWatcher implements android.text.TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void afterTextChanged(android.text.Editable s) {}
    }


}