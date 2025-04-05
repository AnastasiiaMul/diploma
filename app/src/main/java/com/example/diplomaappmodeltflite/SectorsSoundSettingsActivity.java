package com.example.diplomaappmodeltflite;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SectorsSoundSettingsActivity extends AppCompatActivity {
    private int sectorId;
    private String[] availableSounds = {"sector1", "sector2", "sector3", "Фа", "Соль", "Ля", "Сі"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sectors_sound_settings);

        // Get the sector ID from intent
        sectorId = getIntent().getIntExtra("sectorId", 1);

        LinearLayout soundListLayout = findViewById(R.id.soundListLayout);

        // Load current selected sound for this sector
        String selectedSound = SoundPreferences.getSoundForSector(this, sectorId);

        // Dynamically add buttons for each sound
        for (String soundName : availableSounds) {
            Button soundButton = new Button(this);
            soundButton.setText(soundName);

            // Highlight selected sound
            if (soundName.equals(selectedSound)) {
                soundButton.setBackgroundColor(Color.parseColor("#008000"));
            }

            soundButton.setOnClickListener(v -> {
                SoundPreferences.saveSoundForSector(this, sectorId, soundName);
                finish(); // Go back to SectorsSettingsActivity
            });
            soundListLayout.addView(soundButton);
        }

        // Back button
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }
}

