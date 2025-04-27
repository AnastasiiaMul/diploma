package com.example.diplomaappmodeltflite;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SectorsSoundSettingsActivity extends AppCompatActivity {
    private int sectorId;
    private String[] availableSoundsUI = {"До", "Ре", "Мі", "Фа", "Соль", "Ля", "Сі"};
    private String[] availableSounds = {"sector1", "sector2", "sector3", "sector4", "sector5", "sector6", "sector7"};

    private MediaPlayer mediaPlayer;

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
        for (int i = 0; i < availableSounds.length; i++) {
            String soundSystemName = availableSounds[i];
            String soundUIName = availableSoundsUI[i];

            Button soundButton = new Button(this);
            soundButton.setText(soundUIName);

            if (soundSystemName.equals(selectedSound)) {
                soundButton.setBackgroundColor(Color.parseColor("#008000"));
            }

            soundButton.setOnClickListener(v -> {
                playSoundAndAskConfirmation(this, soundSystemName, soundUIName);
            });

            soundListLayout.addView(soundButton);
        }

        // Back button
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void playSoundAndAskConfirmation(Context context, String systemSoundName, String uiSoundName) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        int resId = context.getResources().getIdentifier(systemSoundName.toLowerCase(), "raw", context.getPackageName());
        if (resId != 0) {
            mediaPlayer = MediaPlayer.create(context, resId);
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
                showConfirmationDialog(systemSoundName, uiSoundName);
            });
            mediaPlayer.start();
        } else {
            showConfirmationDialog(systemSoundName, uiSoundName);
        }
    }

    private void showConfirmationDialog(String systemSoundName, String uiSoundName) {
        new AlertDialog.Builder(this)
                .setTitle("Підтвердити звук")
                .setMessage("Обрати \"" + uiSoundName + "\" як звук?")
                .setPositiveButton("Так", (dialog, which) -> {
                    SoundPreferences.saveSoundForSector(this, sectorId, systemSoundName);
                    finish();
                })
                .setNegativeButton("Ні", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}

