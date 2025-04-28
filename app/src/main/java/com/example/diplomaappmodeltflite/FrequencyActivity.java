package com.example.diplomaappmodeltflite;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class FrequencyActivity extends AppCompatActivity {

    private SeekBar seekBarObjectCooldown, seekBarGlobalPause, seekBarDetectionInterval;
    private TextView textViewObjectCooldownValue, textViewGlobalPauseValue, textViewDetectionIntervalValue;
    private Spinner spinnerDetectionMode;
    private Button buttonSave, buttonBack;

    private SharedPreferences preferences;

    private static final String PREFS_NAME = "FrequencyPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frequency);

        seekBarObjectCooldown = findViewById(R.id.seekBarObjectCooldown);
        seekBarGlobalPause = findViewById(R.id.seekBarGlobalPause);
        seekBarDetectionInterval = findViewById(R.id.seekBarDetectionInterval);

        textViewObjectCooldownValue = findViewById(R.id.textViewObjectCooldownValue);
        textViewGlobalPauseValue = findViewById(R.id.textViewGlobalPauseValue);
        textViewDetectionIntervalValue = findViewById(R.id.textViewDetectionIntervalValue);

        spinnerDetectionMode = findViewById(R.id.spinnerDetectionMode);
        buttonSave = findViewById(R.id.buttonSaveFrequency);
        buttonBack = findViewById(R.id.buttonBack);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        setupSpinner();
        loadPreferences();
        setupListeners();

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

        seekBarObjectCooldown.setProgress(objectCooldown);
        seekBarGlobalPause.setProgress(globalPause);
        seekBarDetectionInterval.setProgress(detectionInterval);

        textViewObjectCooldownValue.setText(objectCooldown + " ms");
        textViewGlobalPauseValue.setText(globalPause + " ms");
        textViewDetectionIntervalValue.setText(detectionMode.equals("Seconds")
                ? detectionInterval + " seconds"
                : detectionInterval + " frames");

        int spinnerPosition = detectionMode.equals("Frames") ? 1 : 0;
        spinnerDetectionMode.setSelection(spinnerPosition);
    }

    private void setupListeners() {
        seekBarObjectCooldown.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textViewObjectCooldownValue.setText(progress + " ms");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarGlobalPause.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textViewGlobalPauseValue.setText(progress + " ms");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarDetectionInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String mode = spinnerDetectionMode.getSelectedItem().toString();
                textViewDetectionIntervalValue.setText(mode.equals("Frames")
                        ? progress + " frames"
                        : progress + " seconds");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("object_sound_cooldown_ms", seekBarObjectCooldown.getProgress());
        editor.putInt("global_sound_pause_ms", seekBarGlobalPause.getProgress());
        editor.putInt("detection_interval_value", seekBarDetectionInterval.getProgress());
        editor.putString("detection_interval_mode", spinnerDetectionMode.getSelectedItem().toString());
        editor.apply();
        finish();
    }
}
