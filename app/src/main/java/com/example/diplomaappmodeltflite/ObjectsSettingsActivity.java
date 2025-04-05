package com.example.diplomaappmodeltflite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

public class ObjectsSettingsActivity extends AppCompatActivity {

    private LinearLayout objectsButtonsContainer;
    private String[] objectLabels = {"person", "car", "bicycle", "bench"};
    private Map<String, TextView> soundLabelsMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_objects_settings);

        objectsButtonsContainer = findViewById(R.id.objectsButtonsContainer);

        for (String objectLabel : objectLabels) {
            // Create object button
            Button objectButton = new Button(this);
            objectButton.setText(objectLabel);
            objectButton.setTextSize(20);
            objectButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, ObjectsSoundsSettingsActivity.class);
                intent.putExtra("objectLabel", objectLabel);
                startActivity(intent);
            });

            // Create label for selected sound
            TextView soundLabel = new TextView(this);
            soundLabel.setTextSize(16);
            soundLabel.setPadding(0, 4, 0, 16);

            updateSoundLabelText(objectLabel, soundLabel);
            soundLabelsMap.put(objectLabel, soundLabel);

            // Add views to container
            objectsButtonsContainer.addView(objectButton);
            objectsButtonsContainer.addView(soundLabel);
        }

        // Back button
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh sound labels when coming back from ObjectsSoundsSettingsActivity
        for (String objectLabel : objectLabels) {
            TextView label = soundLabelsMap.get(objectLabel);
            if (label != null) {
                updateSoundLabelText(objectLabel, label);
            }
        }
    }

    private void updateSoundLabelText(String objectLabel, TextView label) {
        String sound = ObjectSoundPreferences.getSoundForObject(this, objectLabel);
        if (sound == null || sound.isEmpty()) {
            sound = "Звук за замовчуванням";
        }
        label.setText("Обраний звук: " + sound);
    }
}

