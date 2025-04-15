package com.example.diplomaappmodeltflite;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class AppSettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppSettingsPrefs";
    private static final String FONT_SIZE_KEY = "font_size";
    private static final String VOLUME_LEVEL_KEY = "volume_level";
    private static final String DARK_MODE_KEY = "dark_mode";

    private SeekBar fontSizeSeekBar, volumeSeekBar;
    private TextView fontPreviewText, volumePreviewText;

    private Switch themeSwitch;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_settings);

        fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar);
        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        themeSwitch = findViewById(R.id.themeSwitch);
        backButton = findViewById(R.id.backButton);

        fontPreviewText = findViewById(R.id.fontPreviewText);
        volumePreviewText = findViewById(R.id.volumePreviewText);


        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        fontSizeSeekBar.setProgress(prefs.getInt(FONT_SIZE_KEY, 16));
        volumeSeekBar.setProgress(prefs.getInt(VOLUME_LEVEL_KEY, 100));
        themeSwitch.setChecked(prefs.getBoolean(DARK_MODE_KEY, true));

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(DARK_MODE_KEY, isChecked);
            editor.apply();

            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prefs.edit().putInt(FONT_SIZE_KEY, progress).apply();
                fontPreviewText.setTextSize(progress);  // live update
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prefs.edit().putInt(VOLUME_LEVEL_KEY, progress).apply();
                volumePreviewText.setText("Гучність: " + progress + "%");  // live update
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        backButton.setOnClickListener(v -> finish());
    }
}
