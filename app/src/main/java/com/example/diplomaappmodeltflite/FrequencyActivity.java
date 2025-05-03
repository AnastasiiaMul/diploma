package com.example.diplomaappmodeltflite;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class FrequencyActivity extends AppCompatActivity {

    private EditText editObjectCooldown, editGlobalPause, editDetectionInterval;
    private Spinner spinnerDetectionMode;
    private Button buttonSave, buttonBack;

    private SharedPreferences preferences;

    private static final String PREFS_NAME = "FrequencyPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frequency);

        editObjectCooldown = findViewById(R.id.editObjectCooldown);
        editGlobalPause = findViewById(R.id.editGlobalPause);
        editDetectionInterval = findViewById(R.id.editDetectionInterval);

        spinnerDetectionMode = findViewById(R.id.spinnerDetectionMode);
        buttonSave = findViewById(R.id.buttonSaveFrequency);
        buttonBack = findViewById(R.id.buttonBack);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        setupSpinner();
        loadPreferences();

        buttonSave.setOnClickListener(v -> savePreferences());
        buttonBack.setOnClickListener(v -> finish());
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.detection_modes_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDetectionMode.setAdapter(adapter);
    }

    private void loadPreferences() {
        int objectCooldown = preferences.getInt("object_sound_cooldown_ms", 2000);
        int globalPause = preferences.getInt("global_sound_pause_ms", 500);
        int detectionInterval = preferences.getInt("detection_interval_value", 10);
        String detectionMode = preferences.getString("detection_interval_mode", "Seconds");

        editObjectCooldown.setText(String.valueOf(objectCooldown));
        editGlobalPause.setText(String.valueOf(globalPause));
        editDetectionInterval.setText(String.valueOf(detectionInterval));

        int spinnerPosition = detectionMode.equals("Frames") ? 1 : 0;
        spinnerDetectionMode.setSelection(spinnerPosition);
    }
    private void savePreferences() {
        try {
            int objectCooldown = Integer.parseInt(editObjectCooldown.getText().toString());
            int globalPause = Integer.parseInt(editGlobalPause.getText().toString());
            int detectionInterval = Integer.parseInt(editDetectionInterval.getText().toString());
            String detectionMode = spinnerDetectionMode.getSelectedItem().toString();

            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("object_sound_cooldown_ms", objectCooldown);
            editor.putInt("global_sound_pause_ms", globalPause);
            editor.putInt("detection_interval_value", detectionInterval);
            editor.putString("detection_interval_mode", detectionMode);
            editor.apply();

            Toast.makeText(this, "Налаштування збережено", Toast.LENGTH_SHORT).show();
            finish();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Некоректне значення", Toast.LENGTH_SHORT).show();
        }
    }
}

