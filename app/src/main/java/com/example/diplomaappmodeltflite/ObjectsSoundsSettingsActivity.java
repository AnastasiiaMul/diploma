package com.example.diplomaappmodeltflite;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class ObjectsSoundsSettingsActivity extends AppCompatActivity {

    private String[] availableSounds = {"car", "person", "bicycle", "bench"};
    private MediaPlayer mediaPlayer;
    private String objectType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_objects_sounds_settings);

        objectType = getIntent().getStringExtra("objectLabel");
        if (objectType == null) {
            finish();
            return;
        }

        LinearLayout layout = findViewById(R.id.objectSoundListLayout);

        String selectedSound = ObjectSoundPreferences.getSoundForObject(this, objectType);

        for (String sound : availableSounds) {
            Button soundButton = new Button(this);
            soundButton.setText(sound);

            if (sound.equals(selectedSound)) {
                soundButton.setBackgroundColor(Color.parseColor("#008000"));
            }

            soundButton.setOnClickListener(v -> {
                playSoundAndAskConfirmation(this, sound);
            });

            layout.addView(soundButton);
        }

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void playSoundAndAskConfirmation(Context context, String soundName) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        int resId = context.getResources().getIdentifier(soundName.toLowerCase(), "raw", context.getPackageName());
        if (resId != 0) {
            mediaPlayer = MediaPlayer.create(context, resId);
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
                showConfirmationDialog(soundName); // show confirmation after sound finishes playing
            });
            mediaPlayer.start();
        } else {
            // If sound not found, immediately show confirmation
            showConfirmationDialog(soundName);
        }
    }

    private void showConfirmationDialog(String soundName) {
        new AlertDialog.Builder(this)
                .setTitle("Підтвердити звук")
                .setMessage("Обрати \"" + soundName + "\" як звук?")
                .setPositiveButton("Так", (dialog, which) -> {
                    ObjectSoundPreferences.saveSoundForObject(this, objectType, soundName);
                    finish();
                })
                .setNegativeButton("Ні", (dialog, which) -> {
                    // Do nothing, stay on the screen
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

