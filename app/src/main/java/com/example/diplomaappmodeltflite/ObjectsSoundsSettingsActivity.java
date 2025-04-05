package com.example.diplomaappmodeltflite;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class ObjectsSoundsSettingsActivity extends AppCompatActivity {

    private String[] availableSounds = {"car", "person", "bicycle", "bench"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_objects_sounds_settings);

        String objectType = getIntent().getStringExtra("objectType");
        LinearLayout layout = findViewById(R.id.objectSoundListLayout);

        String selectedSound = ObjectSoundPreferences.getSoundForObject(this, objectType);

        for (String sound : availableSounds) {
            Button soundButton = new Button(this);
            soundButton.setText(sound);
            if (sound.equals(selectedSound)) {
                soundButton.setBackgroundColor(Color.parseColor("#B0E0E6")); // Highlight selected
            }
            soundButton.setOnClickListener(v -> {
                ObjectSoundPreferences.saveSoundForObject(this, objectType, sound);
                finish();
            });
            layout.addView(soundButton);
        }

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }
}

